package com.larp.editor;

import com.larp.database.InventoryItem;
import com.larp.database.InventoryItemRepository;
import com.larp.logviewer.LogEntry;
import com.larp.logviewer.LogEntryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EditorService {

    private final InventoryItemRepository inventoryItemRepository;
    private final LogEntryRepository logEntryRepository;

    public EditorService(InventoryItemRepository inventoryItemRepository, LogEntryRepository logEntryRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.logEntryRepository = logEntryRepository;
    }

    public InventoryItem validateAndSave(InventoryItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Inventory item cannot be null");
        }
        if (item.getItemCode() == null || item.getItemCode().trim().isEmpty()) {
            throw new IllegalArgumentException("itemCode is required");
        }
        if (item.getDescription() == null || item.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("description is required");
        }
        if (item.getQuantity() == null || item.getQuantity() < 0) {
            throw new IllegalArgumentException("quantity must be zero or more");
        }
        if (item.getStatus() == null || item.getStatus().trim().isEmpty()) {
            throw new IllegalArgumentException("status is required");
        }
        if (item.getDate() == null) {
            throw new IllegalArgumentException("date is required");
        }

        InventoryItem saved = inventoryItemRepository.save(item);

        LogEntry logEntry = new LogEntry();
        logEntry.setMessage("Inventory item saved: " + item.getItemCode());
        logEntry.setTimestamp(LocalDateTime.now());
        logEntry.setLevel("INFO");
        logEntryRepository.save(logEntry);
        return saved;
    }

    public List<InventoryItem> getAllItems() {
        return inventoryItemRepository.findAll();
    }

    public void deleteItem(String itemCode) {
        List<InventoryItem> matches = inventoryItemRepository.findByItemCode(itemCode);
        if (!matches.isEmpty()) {
            inventoryItemRepository.delete(matches.get(matches.size() - 1));
        }
    }
}
