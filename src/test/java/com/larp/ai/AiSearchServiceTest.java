package com.larp.ai;

<<<<<<< HEAD
import com.larp.editor.InventoryStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
=======
import com.larp.database.DatabaseService;
import com.larp.editor.InventoryItem;
import com.larp.logviewer.LogService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.stream.function.StreamBridge;
>>>>>>> 3aee914b11d4fe54347f4e181b6413330c75b1b7

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
<<<<<<< HEAD
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
=======
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
>>>>>>> 3aee914b11d4fe54347f4e181b6413330c75b1b7

class AiSearchServiceTest {

    @Test
    void searchPublishesAiEventAfterReturningSearchResults() {
        AgentTestSupport s = new AgentTestSupport();
        AIRequest request = new AIRequest();
        request.setQuery("iron sword");

        s.search.askAi(request);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(s.streamBridge).send(eq("aiEvents-out-0"), eventCaptor.capture());
        AiEvent event = (AiEvent) eventCaptor.getValue();
        assertEquals("iron sword", event.query());
    }

    @Test
    void searchRanksMatchesAndLeavesOutItemsThatDoNotMatch() {
        AgentTestSupport s = new AgentTestSupport();
        s.addItem("ITEM-1001", "Iron sword", 12, InventoryStatus.AVAILABLE);
        s.addItem("ITEM-1002", "Healing potion", 8, InventoryStatus.SHIPPING);
        s.addItem("ITEM-1009", "War hammer", 6, InventoryStatus.AVAILABLE);

        assertEquals(List.of("ITEM-1002"), codes(s.search.search("healing potions", null, null)));
        assertEquals(List.of(), codes(s.search.search("crossbow", null, null)));
        assertEquals(3, s.search.search("", null, null).size());
    }

    @Test
    void searchUsesTermsTheAgentHasLearned() {
        AgentTestSupport s = new AgentTestSupport();
        s.addItem("ITEM-1001", "Iron sword", 12, InventoryStatus.AVAILABLE);
        s.addItem("ITEM-1007", "Silver dagger", 10, InventoryStatus.AVAILABLE);
        s.terminology.learn("blade", "sword dagger", "test");
        AIRequest request = new AIRequest();
        request.setQuery("blade");

        AIResponse response = s.search.askAi(request);

        assertEquals(2, response.getCount());
        assertEquals(List.of("blade", "sword", "dagger"), response.getSearchTerms());
        assertTrue(response.getSummary().contains("learned terms [sword, dagger]"), response.getSummary());
    }

    private static List<String> codes(List<com.larp.editor.InventoryItem> items) {
        return items.stream().map(com.larp.editor.InventoryItem::getItemCode).toList();
    }

    @Test
    void searchPublishesAiEventAfterReturningSearchResults() {
        DatabaseService databaseService = mock(DatabaseService.class);
        LogService logService = mock(LogService.class);
        StreamBridge streamBridge = mock(StreamBridge.class);
        when(databaseService.findItems("logistics", null, null, null)).thenReturn(List.of());
        when(streamBridge.send(anyString(), any(Object.class))).thenReturn(true);
        AiSearchService service = new AiSearchService(databaseService, logService, streamBridge);
        AIRequest request = new AIRequest();
        request.setQuery("iron sword");

        service.askAi(request);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(streamBridge).send(
                org.mockito.ArgumentMatchers.eq("aiEvents-out-0"),
                (Object) eventCaptor.capture());
        AiEvent event = (AiEvent) eventCaptor.getValue();
        assertEquals("iron sword", event.query());
    }
}
