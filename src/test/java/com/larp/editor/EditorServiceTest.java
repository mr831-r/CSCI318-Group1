package com.larp.editor;

import com.larp.database.DatabaseService;
import com.larp.logviewer.LogService;
import org.springframework.cloud.stream.function.StreamBridge;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

class EditorServiceTest {

    @Test
    void statusOptionsComeFromInventoryStatusEnum() {
        EditorService editorService = new EditorService(
                mock(DatabaseService.class),
                mock(LogService.class),
                mock(StreamBridge.class));

        assertEquals(List.of(InventoryStatus.values()), editorService.getInventoryStatuses());
        assertEquals(InventoryStatus.AVAILABLE, InventoryStatus.fromValue("available"));
        assertThrows(IllegalArgumentException.class, () -> InventoryStatus.fromValue("unknown"));
    }

    @Test
    void seedSampleInventoryAddsExpectedRecords() {
        DatabaseService databaseService = mock(DatabaseService.class);
        LogService logService = mock(LogService.class);
        StreamBridge streamBridge = mock(StreamBridge.class);
        when(streamBridge.send(anyString(), any(Object.class))).thenReturn(true);
        EditorService editorService = new EditorService(databaseService, logService, streamBridge);

        List<InventoryItem> savedItems = new ArrayList<>();
        when(databaseService.createItem(org.mockito.ArgumentMatchers.any(InventoryItem.class)))
                .thenAnswer(invocation -> {
                    InventoryItem item = invocation.getArgument(0);
                    savedItems.add(item);
                    return item;
                });

        editorService.seedSampleInventory();

        assertEquals(20, savedItems.size());
        ArgumentCaptor<InventoryItem> captor = ArgumentCaptor.forClass(InventoryItem.class);
        verify(databaseService, times(20)).createItem(captor.capture());
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(streamBridge, times(20)).send(
                org.mockito.ArgumentMatchers.eq("itemEvents-out-0"),
                (Object) eventCaptor.capture());
        assertEquals("ITEM-1001", savedItems.get(0).getItemCode());
        assertEquals(LocalDate.of(2026, 9, 1).getYear(), savedItems.get(0).getDate().getYear());
        ItemEvent event = (ItemEvent) eventCaptor.getAllValues().get(0);
        assertEquals("ITEM_ADDED", event.type());
        assertEquals("ITEM-1001", event.itemId());
    }
}
