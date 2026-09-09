package com.larp.ai;

import com.larp.database.InventoryItem;
import com.larp.database.InventoryItemRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai")
public class AiController {

    private final InventoryItemRepository inventoryItemRepository;

    public AiController(InventoryItemRepository inventoryItemRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
    }

    @PutMapping
    public ResponseEntity<String> searchInventory(@RequestBody AiRequest request) {
        if (request == null || request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Query is required");
        }

        List<InventoryItem> matches = inventoryItemRepository.findByDescriptionContainingIgnoreCase(request.getQuery());
        if (matches.isEmpty()) {
            return ResponseEntity.ok("No matching stock found for: " + request.getQuery());
        }

        StringBuilder builder = new StringBuilder();
        for (InventoryItem item : matches) {
            builder.append(item.getItemCode())
                    .append(" -> Qty: ")
                    .append(item.getQuantity())
                    .append(", Status: ")
                    .append(item.getStatus())
                    .append("; ");
        }

        return ResponseEntity.ok(builder.toString());
    }
}
