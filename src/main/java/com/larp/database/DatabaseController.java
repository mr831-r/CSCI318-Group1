package com.larp.database;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/database")
public class DatabaseController {

    private final DatabaseService databaseService;

    public DatabaseController(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @PutMapping
    public ResponseEntity<InventoryItem> createItem(@RequestBody InventoryItem item) {
        InventoryItem saved = databaseService.createItem(item);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{filter}")
    public ResponseEntity<List<InventoryItem>> readItems(
            @PathVariable String filter,
            @RequestParam(value = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(value = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        return ResponseEntity.ok(databaseService.findItems(filter, start, end));
    }

    @PostMapping("/{filter}")
    public ResponseEntity<InventoryItem> updateItem(@PathVariable String filter, @RequestBody InventoryItem incoming) {
        return databaseService.updateLatestItem(filter, incoming)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{filter}")
    public ResponseEntity<Void> deleteItem(@PathVariable String filter) {
        if (!databaseService.deleteLatestItem(filter)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }
}
