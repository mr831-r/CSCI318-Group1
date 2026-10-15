package com.larp.editor;

import com.larp.database.DatabaseService;
import com.larp.database.InventoryItem;
import com.larp.events.ItemEvent;
import com.larp.logviewer.LogService;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RestController
@RequestMapping("/editor")
public class EditorService {

    private final DatabaseService databaseService;
    private final LogService logService;
    private final StreamBridge streamBridge;

    public EditorService(DatabaseService databaseService, LogService logService, StreamBridge streamBridge) {
        this.databaseService = databaseService;
        this.logService = logService;
        this.streamBridge = streamBridge;
    }

    public InventoryItem validateAndSave(InventoryItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Inventory item cannot be null");
        }
        if (item.getItemCode() == null || item.getItemCode().trim().isEmpty()) {
            throw new IllegalArgumentException("itemCode is required");
        }
        if (item.getName() == null || item.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("name is required");
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

        boolean updating = item.getId() != null;
        InventoryItem saved = databaseService.createItem(item);

        // Broadcast/log the addition through the centralized LogService
        logService.addItemLog(saved);
        publishItemEvent(updating ? "ITEM_UPDATED" : "ITEM_ADDED", saved.getItemCode());
        return saved;
    }

    public List<InventoryItem> getAllItems() {
        return databaseService.findAllItems();
    }

    /* Controller endpoints moved from EditorController into this service class */

    @PutMapping
    public InventoryItem addOrUpdateItem(@RequestBody InventoryItem item) {
        return validateAndSave(item);
    }

    @PutMapping("/prepopulate")
    public List<InventoryItem> prepopulateInventory(@RequestBody(required = false) List<InventoryItem> items) {
        if (items == null || items.isEmpty()) {
            return seedSampleInventory();
        }

        for (InventoryItem item : items) {
            validateAndSave(item);
        }
        return getAllItems();
    }

    @GetMapping
    public List<InventoryItem> getAllItemsEndpoint() {
        return getAllItems();
    }

    @DeleteMapping
    public void deleteItemByBody(@RequestBody java.util.Map<String, String> payload) {
        String itemCode = payload.getOrDefault("itemCode", payload.get("filter"));
        if (itemCode == null || itemCode.isBlank()) {
            return;
        }
        deleteItem(itemCode);
    }

    @DeleteMapping("/{itemCode}")
    public void deleteItemEndpoint(@PathVariable String itemCode) {
        deleteItem(itemCode);
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
            item.setName(row[1]);
            item.setDescription("Imported: " + row[1]);
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
        List<InventoryItem> matches = databaseService.findItems(itemCode, null, null);
        if (!matches.isEmpty()) {
            InventoryItem toDelete = matches.get(matches.size() - 1);
            boolean deleted = databaseService.deleteLatestItem(itemCode);
            // Broadcast/log the removal through the centralized LogService
            logService.removeItemLog(toDelete);
            if (deleted) {
                publishItemEvent("ITEM_REMOVED", toDelete.getItemCode());
            }
        }
    }

    private void publishItemEvent(String type, String itemId) {
        ItemEvent event = new ItemEvent(type, itemId, Instant.now());
        if (!streamBridge.send("itemEvents-out-0", event)) {
            throw new IllegalStateException("Failed to publish item event " + type);
        }
    }
}
