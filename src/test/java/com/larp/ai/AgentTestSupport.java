package com.larp.ai;

import com.larp.database.DatabaseService;
import com.larp.database.IDatabaseItem;
import com.larp.editor.EditorService;
import com.larp.editor.InventoryItem;
import com.larp.editor.InventoryStatus;
import com.larp.logviewer.LogService;
import com.larp.shipping.ShippingRequest;
import com.larp.shipping.ShippingService;
import com.larp.shipping.ShippingStatus;
import org.springframework.cloud.stream.function.StreamBridge;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/** Wires the agent classes together around mocked LARP services, without Spring or Kafka. */
class AgentTestSupport {

    final DatabaseService databaseService = mock(DatabaseService.class);
    final LogService logService = mock(LogService.class);
    final ShippingService shippingService = mock(ShippingService.class);
    final EditorService editorService = mock(EditorService.class);
    final StreamBridge streamBridge = mock(StreamBridge.class);

    final List<InventoryItem> inventory = new ArrayList<>();
    final List<TermAlias> aliases = new ArrayList<>();
    final List<ShippingRequest> shipping = new ArrayList<>();

    final AgentEventPublisher events = new AgentEventPublisher(streamBridge);
    final TerminologyService terminology = new TerminologyService(databaseService);
    final AiSearchService search = new AiSearchService(databaseService, streamBridge, terminology);
    final AgentActionService actions = new AgentActionService(databaseService, editorService, shippingService, events);
    final AgentTools tools = new AgentTools(
            databaseService, logService, shippingService, terminology, search, actions, events);
    final AgentSettings settings = new AgentSettings("gemini", "test-key", "", "", 10, 20);

    AgentTestSupport() {
        lenient().when(streamBridge.send(anyString(), any(Object.class))).thenReturn(true);
        lenient().when(databaseService.findItems(eq("logistics"), isNull(), isNull(), isNull()))
                .thenAnswer(invocation -> new ArrayList<>(inventory));
        lenient().when(databaseService.findLatestItem(eq("logistics"), anyString()))
                .thenAnswer(invocation -> inventory.stream()
                        .filter(item -> item.getItemCode().equals(invocation.getArgument(1)))
                        .findFirst()
                        .map(IDatabaseItem.class::cast));
        lenient().when(databaseService.findItems(eq(TermAlias.FILTER_NAME), isNull(), isNull(), isNull()))
                .thenAnswer(invocation -> new ArrayList<>(aliases));
        lenient().when(databaseService.findLatestItem(eq(TermAlias.FILTER_NAME), anyString()))
                .thenAnswer(invocation -> aliases.stream()
                        .filter(alias -> alias.getTerm().equals(invocation.getArgument(1)))
                        .findFirst()
                        .map(IDatabaseItem.class::cast));
        lenient().when(databaseService.createItem(any())).thenAnswer(invocation -> {
            Object item = invocation.getArgument(0);
            if (item instanceof TermAlias alias) {
                aliases.add(alias);
            }
            return item;
        });
        lenient().when(editorService.validateAndSave(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(shippingService.getShippingRequests()).thenAnswer(invocation -> new ArrayList<>(shipping));
        lenient().when(shippingService.getRequestQueue(any())).thenAnswer(invocation -> {
            ShippingStatus status = invocation.getArgument(0);
            return shipping.stream()
                    .filter(request -> status == null
                            ? request.getStatus() != ShippingStatus.ARRIVED : request.getStatus() == status)
                    .sorted(java.util.Comparator.comparing(ShippingRequest::getRequestedDate)
                            .thenComparing(ShippingRequest::getId))
                    .toList();
        });
        lenient().when(databaseService.findItem(eq("shipping"), any())).thenAnswer(invocation -> shipping.stream()
                .filter(request -> request.getId().equals(invocation.getArgument(1)))
                .findFirst()
                .map(IDatabaseItem.class::cast));
        lenient().when(logService.findLogs(any(), any())).thenReturn(List.of());
    }

    ShippingRequest addRequest(long id, String code, String destination, int quantity, LocalDate date) {
        ShippingRequest request = new ShippingRequest();
        request.setId(id);
        request.setItemCode(code);
        request.setDestination(destination);
        request.setQuantity(quantity);
        request.setRequestedDate(date);
        request.setStatus(ShippingStatus.PENDING);
        shipping.add(request);
        return request;
    }

    AgentRuntime runtime(ScriptedChatModel model) {
        return new AgentRuntime(settings, tools, model, "scripted");
    }

    InventoryItem addItem(String code, String name, int quantity, InventoryStatus status) {
        InventoryItem item = new InventoryItem();
        item.setId((long) inventory.size() + 1);
        item.setItemCode(code);
        item.setName(name);
        item.setDescription("Imported: " + name);
        item.setQuantity(quantity);
        item.setStatus(status);
        item.setDate(LocalDate.of(2026, 9, 1));
        item.setPayslipReference("FAKE-" + code);
        inventory.add(item);
        return item;
    }
}
