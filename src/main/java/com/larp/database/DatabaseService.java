package com.larp.database;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class DatabaseService {

    private final InventoryItemRepository inventoryItemRepository;

    public DatabaseService(InventoryItemRepository inventoryItemRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
    }

    public InventoryItem createItem(InventoryItem item) {
        return inventoryItemRepository.save(item);
    }

    public List<InventoryItem> findItems(String filter, LocalDate start, LocalDate end) {
        if (start != null && end != null) {
            List<InventoryItem> history = inventoryItemRepository.findByItemCodeBetweenDates(filter, start, end);
            if (!history.isEmpty()) {
                return history;
            }
        }

        return inventoryItemRepository.findByItemCode(filter);
    }

    @Transactional
    public Optional<InventoryItem> updateLatestItem(String filter, InventoryItem incoming) {
        List<InventoryItem> existingItems = inventoryItemRepository.findByItemCode(filter);
        if (existingItems.isEmpty()) {
            return Optional.empty();
        }

        InventoryItem existing = existingItems.get(existingItems.size() - 1);
        existing.setDescription(incoming.getDescription() != null ? incoming.getDescription() : existing.getDescription());
        existing.setQuantity(incoming.getQuantity() != null ? incoming.getQuantity() : existing.getQuantity());
        existing.setStatus(incoming.getStatus() != null ? incoming.getStatus() : existing.getStatus());
        existing.setDate(incoming.getDate() != null ? incoming.getDate() : existing.getDate());
        existing.setPayslipReference(incoming.getPayslipReference() != null ? incoming.getPayslipReference() : existing.getPayslipReference());

        return Optional.of(inventoryItemRepository.save(existing));
    }

    public boolean deleteLatestItem(String filter) {
        List<InventoryItem> items = inventoryItemRepository.findByItemCode(filter);
        if (items.isEmpty()) {
            return false;
        }

        inventoryItemRepository.delete(items.get(items.size() - 1));
        return true;
    }
}