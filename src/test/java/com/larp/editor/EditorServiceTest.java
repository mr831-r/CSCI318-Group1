package com.larp.editor;

import com.larp.database.InventoryItem;
import com.larp.database.InventoryItemRepository;
import com.larp.logviewer.LogEntryRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EditorServiceTest {

    @Test
    void seedSampleInventoryAddsExpectedRecords() {
        InventoryItemRepository inventoryItemRepository = mock(InventoryItemRepository.class);
        LogEntryRepository logEntryRepository = mock(LogEntryRepository.class);
        EditorService editorService = new EditorService(inventoryItemRepository, logEntryRepository);

        List<InventoryItem> savedItems = new ArrayList<>();
        when(inventoryItemRepository.save(org.mockito.ArgumentMatchers.any(InventoryItem.class)))
                .thenAnswer(invocation -> {
                    InventoryItem item = invocation.getArgument(0);
                    savedItems.add(item);
                    return item;
                });

        editorService.seedSampleInventory();

        assertEquals(20, savedItems.size());
        ArgumentCaptor<InventoryItem> captor = ArgumentCaptor.forClass(InventoryItem.class);
        verify(inventoryItemRepository, times(20)).save(captor.capture());
        assertEquals("ITEM-1001", savedItems.get(0).getItemCode());
        assertEquals(LocalDate.now().getYear(), savedItems.get(0).getDate().getYear());
    }
}
