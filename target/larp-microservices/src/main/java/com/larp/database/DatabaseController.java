package com.larp.database;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/database")
public class DatabaseController {

    private final InventoryItemRepository inventoryItemRepository;

    public DatabaseController(InventoryItemRepository inventoryItemRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
    }

    @PutMapping
    public ResponseEntity<InventoryItem> createItem(@RequestBody InventoryItem item) {
        InventoryItem saved = inventoryItemRepository.save(item);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{filter}")
    public ResponseEntity<List<InventoryItem>> readItems(
            @PathVariable String filter,
            @RequestParam(value = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(value = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        if (start != null && end != null) {
            List<InventoryItem> history = inventoryItemRepository.findByItemCodeBetweenDates(filter, start, end);
            if (!history.isEmpty()) {
                return ResponseEntity.ok(history);
            }
        }

        return ResponseEntity.ok(inventoryItemRepository.findByItemCode(filter));
    }

    @PostMapping("/{filter}")
    public ResponseEntity<InventoryItem> updateItem(@PathVariable String filter, @RequestBody InventoryItem incoming) {
        List<InventoryItem> existingItems = inventoryItemRepository.findByItemCode(filter);
        if (existingItems.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        InventoryItem existing = existingItems.get(existingItems.size() - 1);
        existing.setDescription(incoming.getDescription() != null ? incoming.getDescription() : existing.getDescription());
        existing.setQuantity(incoming.getQuantity() != null ? incoming.getQuantity() : existing.getQuantity());
        existing.setStatus(incoming.getStatus() != null ? incoming.getStatus() : existing.getStatus());
        existing.setDate(incoming.getDate() != null ? incoming.getDate() : existing.getDate());
        existing.setPayslipReference(incoming.getPayslipReference() != null ? incoming.getPayslipReference() : existing.getPayslipReference());

        return ResponseEntity.ok(inventoryItemRepository.save(existing));
    }

    @DeleteMapping("/{filter}")
    public ResponseEntity<Void> deleteItem(@PathVariable String filter) {
        List<InventoryItem> items = inventoryItemRepository.findByItemCode(filter);
        if (items.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        inventoryItemRepository.delete(items.get(items.size() - 1));
        return ResponseEntity.noContent().build();
    }
}
