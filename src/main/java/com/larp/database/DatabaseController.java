package com.larp.database;

import com.larp.editor.InventoryItem;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    public InventoryItem createInventoryItem(@RequestBody InventoryItem item) {
        return databaseService.createItem(item);
    }

    @PutMapping("/{filter}")
    public InventoryItem createInventoryItemWithFilter(
            @PathVariable String filter,
            @RequestBody InventoryItem item) {
        if (item.getItemCode() == null || item.getItemCode().isBlank()) {
            item.setItemCode(filter);
        }
        return databaseService.createItem(item);
    }

    @GetMapping("/{filter}")
    public List<InventoryItem> readInventoryItems(
            @PathVariable String filter,
            @RequestParam(value = "start", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(value = "end", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return databaseService.findItems(InventoryItem.class, filter, start, end);
    }

    @PostMapping("/{filter}")
    public InventoryItem updateLatestInventoryItem(
            @PathVariable String filter,
            @RequestBody InventoryItem incoming) {
        return databaseService.findLatestItem(InventoryItem.class, filter)
                .flatMap(existing -> databaseService.updateItem(
                        InventoryItem.class,
                        existing.getId(),
                        incoming))
                .orElse(null);
    }

    @DeleteMapping("/{filter}")
    public void deleteLatestInventoryItem(@PathVariable String filter) {
        databaseService.findLatestItem(InventoryItem.class, filter)
                .ifPresent(item -> databaseService.deleteItem(InventoryItem.class, item.getId()));
    }
}
