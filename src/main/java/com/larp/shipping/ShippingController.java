package com.larp.shipping;

import com.larp.database.InventoryItem;
import com.larp.database.InventoryItemRepository;
import com.larp.logviewer.LogEntry;
import com.larp.logviewer.LogEntryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/shipping")
public class ShippingController {

    private final ShippingRequestRepository shippingRequestRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final LogEntryRepository logEntryRepository;

    public ShippingController(ShippingRequestRepository shippingRequestRepository,
                             InventoryItemRepository inventoryItemRepository,
                             LogEntryRepository logEntryRepository) {
        this.shippingRequestRepository = shippingRequestRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.logEntryRepository = logEntryRepository;
    }

    @PutMapping
    public ResponseEntity<ShippingRequest> confirmArrival(@RequestBody ShippingRequest request) {
        if (request == null || request.getItemCode() == null || request.getItemCode().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        List<InventoryItem> matchingItems = inventoryItemRepository.findByItemCode(request.getItemCode());
        if (!matchingItems.isEmpty()) {
            InventoryItem latestItem = matchingItems.get(matchingItems.size() - 1);
            latestItem.setStatus("ARRIVED");
            if (request.getQuantity() != null) {
                latestItem.setQuantity(request.getQuantity());
            }
            if (request.getRequestedDate() != null) {
                latestItem.setDate(request.getRequestedDate());
            }
            inventoryItemRepository.save(latestItem);
        }

        ShippingRequest saved = shippingRequestRepository.save(request);

        LogEntry logEntry = new LogEntry();
        logEntry.setMessage("Shipping confirmation received for item: " + request.getItemCode());
        logEntry.setLevel("INFO");
        logEntry.setTimestamp(LocalDateTime.now());
        logEntryRepository.save(logEntry);

        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public ResponseEntity<List<ShippingRequest>> getShippingRequests() {
        return ResponseEntity.ok(shippingRequestRepository.findAll());
    }
}
