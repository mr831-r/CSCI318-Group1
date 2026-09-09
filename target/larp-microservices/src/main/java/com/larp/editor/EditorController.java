package com.larp.editor;

import com.larp.database.InventoryItem;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping
    public ResponseEntity<List<InventoryItem>> getAllItems() {
        return ResponseEntity.ok(editorService.getAllItems());
    }

    @DeleteMapping("/{itemCode}")
    public ResponseEntity<Void> deleteItem(@PathVariable String itemCode) {
        editorService.deleteItem(itemCode);
        return ResponseEntity.noContent().build();
    }
}
