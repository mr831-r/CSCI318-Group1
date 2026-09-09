package com.larp.editor;

import com.larp.database.InventoryItem;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/editor")
public class EditorController {

    private final EditorService editorService;

    public EditorController(EditorService editorService) {
        this.editorService = editorService;
    }

    @PutMapping
    public ResponseEntity<InventoryItem> addOrUpdateItem(@RequestBody InventoryItem item) {
        InventoryItem saved = editorService.validateAndSave(item);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/prepopulate")
    public ResponseEntity<List<InventoryItem>> prepopulateInventory(@RequestBody List<InventoryItem> items) {
        if (items == null || items.isEmpty()) {
            return ResponseEntity.ok(editorService.seedSampleInventory());
        }

        for (InventoryItem item : items) {
            editorService.validateAndSave(item);
        }
        return ResponseEntity.ok(editorService.getAllItems());
    }

    @GetMapping
    public ResponseEntity<List<InventoryItem>> getAllItems() {
        return ResponseEntity.ok(editorService.getAllItems());
    }

    @PostMapping("/seed")
    public ResponseEntity<List<InventoryItem>> seedSampleInventory() {
        return ResponseEntity.ok(editorService.seedSampleInventory());
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteItemByBody(@RequestBody Map<String, String> payload) {
        String itemCode = payload.getOrDefault("itemCode", payload.get("filter"));
        if (itemCode == null || itemCode.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        editorService.deleteItem(itemCode);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{itemCode}")
    public ResponseEntity<Void> deleteItem(@PathVariable String itemCode) {
        editorService.deleteItem(itemCode);
        return ResponseEntity.noContent().build();
    }
}
