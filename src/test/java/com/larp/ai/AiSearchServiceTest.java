package com.larp.ai;

import com.larp.database.DatabaseService;
import com.larp.editor.InventoryItem;
import com.larp.events.AiEvent;
import com.larp.logviewer.LogService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.stream.function.StreamBridge;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiSearchServiceTest {

    @Test
    void defaultModelAndProviderUseGemini() {
        assertEquals("gemini-2.0-flash", AiSearchService.defaultModel());
        assertEquals(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent",
                AiSearchService.defaultProviderUrl()
        );
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
