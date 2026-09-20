package com.larp.database;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatabaseServiceTest {

    @Test
    void updateLatestItemMergesOnlyProvidedFields() {
        InventoryItemRepository repository = mock(InventoryItemRepository.class);
        InventoryItem existing = new InventoryItem();
        existing.setItemCode("ITEM-1");
        existing.setDescription("Old description");
        existing.setQuantity(2);
        existing.setStatus("AVAILABLE");
        InventoryItem incoming = new InventoryItem();
        incoming.setQuantity(5);
        when(repository.findByItemCode("ITEM-1")).thenReturn(List.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        DatabaseService service = new DatabaseService(repository);

        assertEquals(existing, service.updateLatestItem("ITEM-1", incoming).orElseThrow());
        assertEquals("Old description", existing.getDescription());
        assertEquals(5, existing.getQuantity());
        assertEquals("AVAILABLE", existing.getStatus());
        verify(repository).save(existing);
    }

    @Test
    void deleteLatestItemReturnsFalseWhenNoRecordMatches() {
        InventoryItemRepository repository = mock(InventoryItemRepository.class);
        when(repository.findByItemCode("missing")).thenReturn(List.of());

        assertFalse(new DatabaseService(repository).deleteLatestItem("missing"));
    }
}