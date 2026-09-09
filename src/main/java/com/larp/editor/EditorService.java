package com.larp.editor;

import com.larp.database.InventoryItem;
import com.larp.database.InventoryItemRepository;
import com.larp.logviewer.LogEntry;
import com.larp.logviewer.LogEntryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    public List<InventoryItem> seedSampleInventory() {
        List<InventoryItem> sampleItems = new ArrayList<>();

        String[][] seedData = {
                {"ITEM-1001", "Iron sword", "12", "available", "2026-09-01", "FAKE-INV-1001"},
                {"ITEM-1002", "Healing potion", "8", "shipping", "2026-09-02", "FAKE-INV-1002"},
                {"ITEM-1003", "Steel shield", "4", "available", "2026-09-03", "FAKE-INV-1003"},
                {"ITEM-1004", "Bronze axe", "15", "shipping", "2026-09-04", "FAKE-INV-1004"},
                {"ITEM-1005", "Leather armour", "9", "available", "2026-09-05", "FAKE-INV-1005"},
                {"ITEM-1006", "Moonstone ring", "3", "shipping", "2026-09-06", "FAKE-INV-1006"},
                {"ITEM-1007", "Silver dagger", "10", "available", "2026-09-01", "FAKE-INV-1007"},
                {"ITEM-1008", "Mana crystal", "20", "shipping", "2026-09-07", "FAKE-INV-1008"},
                {"ITEM-1009", "War hammer", "6", "available", "2026-09-08", "FAKE-INV-1009"},
                {"ITEM-1010", "Chainmail vest", "11", "shipping", "2026-09-09", "FAKE-INV-1010"},
                {"ITEM-1011", "Torch bundle", "30", "available", "2026-09-09", "FAKE-INV-1011"},
                {"ITEM-1012", "Rope coil", "25", "shipping", "2026-09-02", "FAKE-INV-1012"},
                {"ITEM-1013", "Wizard staff", "5", "available", "2026-09-03", "FAKE-INV-1013"},
                {"ITEM-1014", "Throwing knife", "18", "shipping", "2026-09-05", "FAKE-INV-1014"},
                {"ITEM-1015", "Hunter bow", "7", "available", "2026-09-04", "FAKE-INV-1015"},
                {"ITEM-1016", "Elven cloak", "13", "shipping", "2026-09-06", "FAKE-INV-1016"},
                {"ITEM-1017", "Curing herbs", "40", "available", "2026-09-07", "FAKE-INV-1017"},
                {"ITEM-1018", "Sapphire gem", "2", "shipping", "2026-09-08", "FAKE-INV-1018"},
                {"ITEM-1019", "Cavalry lance", "9", "available", "2026-09-09", "FAKE-INV-1019"},
                {"ITEM-1020", "Rune shield", "14", "shipping", "2026-09-10", "FAKE-INV-1020"}
        };

        for (String[] row : seedData) {
            InventoryItem item = new InventoryItem();
            item.setItemCode(row[0]);
            item.setDescription(row[1]);
            item.setQuantity(Integer.parseInt(row[2]));
            item.setStatus(row[3]);
            item.setDate(LocalDate.parse(row[4]));
            item.setPayslipReference(row[5]);
            sampleItems.add(item);
        }

        List<InventoryItem> savedItems = new ArrayList<>();
        for (InventoryItem item : sampleItems) {
            savedItems.add(validateAndSave(item));
        }

        return savedItems;
    }

    public void deleteItem(String itemCode) {
        List<InventoryItem> matches = inventoryItemRepository.findByItemCode(itemCode);
        if (!matches.isEmpty()) {
            inventoryItemRepository.delete(matches.get(matches.size() - 1));
        }
    }
}
