package com.larp.ai;

import com.larp.editor.InventoryItem;
import com.larp.editor.InventoryStatus;
import com.larp.shipping.ShippingRequest;
import com.larp.shipping.ShippingStatus;
import com.larp.shipping.StockEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentServiceTest {

    @Test
    void agentCallsSearchToolAndAnswersFromItsResult() {
        AgentTestSupport s = new AgentTestSupport();
        s.addItem("ITEM-1001", "Iron sword", 12, InventoryStatus.AVAILABLE);
        s.addItem("ITEM-1002", "Healing potion", 8, InventoryStatus.SHIPPING);
        ScriptedChatModel model = new ScriptedChatModel()
                .callsTool("searchInventory", "{\"query\":\"sword\"}")
                .answersWithLastToolResult("Here is what I found: ");
        AgentService service = new AgentService(s.runtime(model), s.actions, s.events);

        AgentService.AgentChatResponse response = service.chat("s1", "Do we have any swords?");

        assertTrue(response.configured());
        assertEquals("s1", response.sessionId());
        assertEquals(1, response.steps().size());
        assertEquals("searchInventory", response.steps().get(0).tool());
        assertTrue(response.reply().contains("ITEM-1001"), response.reply());
        assertFalse(response.reply().contains("ITEM-1002"), response.reply());
        // The model was given the tools and a system prompt
        assertFalse(model.requests.get(0).toolSpecifications().isEmpty());
        // The conversation was audited on the agent-events topic
        verify(s.streamBridge).send(eq(AgentEventPublisher.BINDING), any(Object.class));
    }

    @Test
    void agentChainsSeveralToolsAndLearnsNewTerminology() {
        AgentTestSupport s = new AgentTestSupport();
        s.addItem("ITEM-1001", "Iron sword", 12, InventoryStatus.AVAILABLE);
        s.addItem("ITEM-1007", "Silver dagger", 10, InventoryStatus.AVAILABLE);
        ScriptedChatModel model = new ScriptedChatModel()
                .callsTool("searchInventory", "{\"query\":\"blade\"}")
                .callsTool("learnTerm", "{\"term\":\"blade\",\"meaning\":\"sword dagger\"}")
                .callsTool("searchInventory", "{\"query\":\"blade\"}")
                .answersWithLastToolResult("");
        AgentService service = new AgentService(s.runtime(model), s.actions, s.events);

        AgentService.AgentChatResponse response = service.chat("s2", "Any blades? Blade means sword or dagger.");

        assertEquals(List.of("searchInventory", "learnTerm", "searchInventory"),
                response.steps().stream().map(AgentService.AgentStep::tool).toList());
        assertTrue(response.steps().get(0).result().startsWith("No inventory items matched"));
        assertTrue(response.reply().contains("ITEM-1001") && response.reply().contains("ITEM-1007"), response.reply());
        assertEquals(1, s.aliases.size());
        assertEquals("s2", s.aliases.get(0).getLearnedFrom());
    }

    @Test
    void writeToolsOnlyProposeUntilAPersonApproves() {
        AgentTestSupport s = new AgentTestSupport();
        s.addItem("ITEM-1003", "Steel shield", 4, InventoryStatus.AVAILABLE);
        ScriptedChatModel model = new ScriptedChatModel()
                .callsTool("proposeQuantityChange",
                        "{\"itemCode\":\"ITEM-1003\",\"newQuantity\":2,\"reason\":\"Two shields were damaged\"}")
                .answersWithLastToolResult("");
        AgentService service = new AgentService(s.runtime(model), s.actions, s.events);

        AgentService.AgentChatResponse response = service.chat("s3", "Two shields broke, fix the count");

        assertEquals(1, response.pendingActions().size());
        PendingAction action = response.pendingActions().get(0);
        assertEquals(PendingAction.Type.SET_QUANTITY, action.type());
        assertEquals(2, action.quantity());
        verify(s.editorService, never()).validateAndSave(any());

        PendingAction approved = service.approve(action.id());

        assertEquals(PendingAction.State.APPROVED, approved.state());
        ArgumentCaptor<InventoryItem> saved = ArgumentCaptor.forClass(InventoryItem.class);
        verify(s.editorService).validateAndSave(saved.capture());
        assertEquals(2, saved.getValue().getQuantity());
        assertTrue(s.actions.pending("s3").isEmpty());
    }

    @Test
    void rejectedProposalsNeverReachTheEditor() {
        AgentTestSupport s = new AgentTestSupport();
        s.addItem("ITEM-1003", "Steel shield", 4, InventoryStatus.AVAILABLE);
        PendingAction action = s.actions.proposeStatus("s4", "ITEM-1003", "shipping", "Sent to camp");

        PendingAction rejected = s.actions.reject(action.id());

        assertEquals(PendingAction.State.REJECTED, rejected.state());
        verify(s.editorService, never()).validateAndSave(any());
    }

    @Test
    void toolErrorsAreReturnedToTheModelInsteadOfFailingTheRequest() {
        AgentTestSupport s = new AgentTestSupport();
        String result = s.tools.proposeQuantityChange("s5", "ITEM-9999", 3, "typo");
        assertTrue(result.startsWith("ERROR: Unknown inventory item code"), result);
        assertTrue(s.actions.pending("s5").isEmpty());
    }

    @Test
    void reportsWhenNoModelIsConfigured() {
        AgentTestSupport s = new AgentTestSupport();
        AgentRuntime runtime = new AgentRuntime(s.settings, s.tools, null, "none");
        AgentService service = new AgentService(runtime, s.actions, s.events);

        AgentService.AgentChatResponse response = service.chat(null, "hello");

        assertFalse(response.configured());
        assertEquals(AgentService.NOT_CONFIGURED, response.reply());
        assertFalse(response.sessionId().isBlank());
    }

    @Test
    void stockEventAgentAllocatesArrivedStockToOldestRequestsFirst() {
        AgentTestSupport s = new AgentTestSupport();
        s.addItem("ITEM-1002", "Healing potion", 10, InventoryStatus.ARRIVED);
        s.addRequest(1, "ITEM-1002", "South Camp", 8, LocalDate.of(2026, 9, 5));
        s.addRequest(2, "ITEM-1002", "North Camp", 6, LocalDate.of(2026, 9, 1));
        s.addRequest(3, "ITEM-9999", "Elsewhere", 1, LocalDate.of(2026, 9, 1));
        AgentRuntime unconfigured = new AgentRuntime(s.settings, s.tools, null, "none");
        StockEventAgent agent = new StockEventAgent(s.databaseService, s.shippingService, unconfigured, s.events, true);

        String summary = agent.handle(new StockEvent("STOCK_ARRIVED", "ITEM-1002", 5, Instant.now()));

        assertTrue(summary.contains("2 pending request(s)"), summary);
        assertTrue(summary.indexOf("North Camp") < summary.indexOf("South Camp"), summary);
        assertTrue(summary.contains("can fill 6 for North Camp"), summary);
        assertTrue(summary.contains("short by 4 for South Camp"), summary);
    }

    @Test
    void stockEventAgentAsksTheLlmForARecommendationWhenConfigured() {
        AgentTestSupport s = new AgentTestSupport();
        s.addItem("ITEM-1002", "Healing potion", 10, InventoryStatus.ARRIVED);
        ScriptedChatModel model = new ScriptedChatModel()
                .callsTool("getItemHistory", "{\"itemCode\":\"ITEM-1002\"}")
                .answers("Stock is healthy; no action needed.");
        StockEventAgent agent = new StockEventAgent(s.databaseService, s.shippingService, s.runtime(model), s.events, true);

        agent.handle(new StockEvent("STOCK_ARRIVED", "ITEM-1002", 5, Instant.now()));

        assertEquals(2, model.requests.size());
        assertTrue(model.requests.get(0).messages().toString().contains("STOCK_ARRIVED"));
    }

    @Test
    void dispatchProposalRunsThroughShippingServiceWhenApproved() {
        AgentTestSupport s = new AgentTestSupport();
        s.addItem("ITEM-1002", "Healing potion", 10, InventoryStatus.ARRIVED);
        s.addRequest(7, "ITEM-1002", "North Camp", 6, LocalDate.of(2026, 9, 1));
        ShippingRequest dispatched = new ShippingRequest();
        dispatched.setId(7L);
        dispatched.setCourier("Fast Horse");
        when(s.shippingService.dispatch(7L, "Fast Horse")).thenReturn(dispatched);
        ScriptedChatModel model = new ScriptedChatModel()
                .callsTool("listShipments", "{\"status\":\"PENDING\"}")
                .callsTool("proposeDispatch", "{\"requestId\":7,\"courier\":\"Fast Horse\",\"reason\":\"Oldest request\"}")
                .answersWithLastToolResult("");
        AgentService service = new AgentService(s.runtime(model), s.actions, s.events);

        AgentService.AgentChatResponse response = service.chat("s6", "Send whatever we can");

        assertTrue(response.steps().get(0).result().contains("request 7 | ITEM-1002"), response.steps().get(0).result());
        PendingAction action = response.pendingActions().get(0);
        assertEquals(PendingAction.Type.DISPATCH_REQUEST, action.type());
        verify(s.shippingService, never()).dispatch(any(), any());

        assertEquals(PendingAction.State.APPROVED, service.approve(action.id()).state());
        verify(s.shippingService).dispatch(7L, "Fast Horse");
    }

    @Test
    void onlyPendingRequestsCanBeProposedForDispatch() {
        AgentTestSupport s = new AgentTestSupport();
        s.addRequest(8, "ITEM-1002", "North Camp", 6, LocalDate.of(2026, 9, 1)).setStatus(ShippingStatus.DISPATCHED);
        String result = s.tools.proposeDispatch("s7", 8, "Fast Horse", "again");
        assertTrue(result.startsWith("ERROR: Shipping request 8 is DISPATCHED"), result);
    }

    @Test
    void dispatchIsNotProposedWithoutEnoughStock() {
        AgentTestSupport s = new AgentTestSupport();
        s.addItem("ITEM-1002", "Healing potion", 3, InventoryStatus.ARRIVED);
        s.addRequest(9, "ITEM-1002", "East Camp", 50, LocalDate.of(2026, 9, 1));

        String result = s.tools.proposeDispatch("s8", 9, "Fast Horse", "oldest");

        assertTrue(result.startsWith("ERROR: Not enough stock to dispatch request 9: 3 x ITEM-1002 available"), result);
    }

    @Test
    void identicalProposalsAreNotStackedAndApprovalSupersedesOthers() {
        AgentTestSupport s = new AgentTestSupport();
        s.addItem("ITEM-1002", "Healing potion", 10, InventoryStatus.ARRIVED);
        s.addRequest(10, "ITEM-1002", "North Camp", 6, LocalDate.of(2026, 9, 1));
        when(s.shippingService.dispatch(any(), any())).thenReturn(new ShippingRequest());

        PendingAction first = s.actions.proposeDispatch("autonomous", 10, "Fast Horse", "review 1");
        PendingAction again = s.actions.proposeDispatch("autonomous", 10, "Fast Horse", "review 2");
        PendingAction otherCourier = s.actions.proposeDispatch("demo", 10, "Slow Cart", "user asked");

        assertEquals(first.id(), again.id());
        assertEquals(2, s.actions.pending(null).size());
        s.actions.approve(otherCourier.id());
        assertEquals(PendingAction.State.REJECTED, s.actions.find(first.id()).orElseThrow().state());
        assertTrue(s.actions.pending(null).isEmpty());
    }
}
