package com.larp.editor;

import com.larp.database.DatabaseService;
import com.larp.database.InventoryItem;
import com.larp.logviewer.LogService;
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
        DatabaseService databaseService = mock(DatabaseService.class);
        LogService logService = mock(LogService.class);
        EditorService editorService = new EditorService(databaseService, logService);

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
        assertEquals("ITEM-1001", savedItems.get(0).getItemCode());
        assertEquals(LocalDate.of(2026, 9, 1).getYear(), savedItems.get(0).getDate().getYear());
    }
}
