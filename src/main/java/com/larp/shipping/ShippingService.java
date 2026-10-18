package com.larp.shipping;

import com.larp.database.DatabaseService;
import com.larp.editor.InventoryItem;
import com.larp.editor.InventoryStatus;
import com.larp.logviewer.LogService;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RestController
@RequestMapping("/shipping")
public class ShippingService {

    private final DatabaseService databaseService;
    private final LogService logService;
    private final StreamBridge streamBridge;

    public ShippingService(
            DatabaseService databaseService,
            LogService logService,
            StreamBridge streamBridge) {
        this.databaseService = databaseService;
        this.logService = logService;
        this.streamBridge = streamBridge;
    }

    @Transactional
    public Optional<ShippingRequest> confirmArrival(ShippingRequest request) {
        if (request == null || request.getItemCode() == null || request.getItemCode().isBlank()) {
            return Optional.empty();
        }
        if (request.getQuantity() == null || request.getQuantity() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arrival quantity must be zero or more");
        }
        if (request.getRequestedDate() == null || request.getDestination() == null
                || request.getDestination().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arrival date and destination are required");
        }

        if (databaseService.mutateLatestItem("logistics", request.getItemCode(), item -> {
            if (!(item instanceof InventoryItem inventoryItem)) {
                throw new IllegalStateException("Logistics record is not an inventory item");
            }
            try {
                inventoryItem.setQuantity(Math.addExact(inventoryItem.getQuantity(), request.getQuantity()));
            } catch (ArithmeticException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arrival quantity exceeds supported stock");
            }
            inventoryItem.setStatus(InventoryStatus.ARRIVED);
            inventoryItem.setDate(request.getRequestedDate());
        }).isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unknown inventory item code: " + request.getItemCode());
        }

        request.setStatus(ShippingStatus.ARRIVED);
        ShippingRequest saved = ShippingRequest.class.cast(databaseService.createItem(request));
        logService.recordShippingConfirmation(request.getItemCode());
        int quantityDelta = request.getQuantity();
        StockEvent event = new StockEvent(
                "STOCK_ARRIVED",
                request.getItemCode(),
                quantityDelta,
                Instant.now());
        boolean published = streamBridge.send(
                "stockEvents-out-0",
                MessageBuilder.withPayload(event)
                        .setHeader(KafkaHeaders.KEY, event.itemId())
                        .build());
        if (!published) {
            throw new IllegalStateException("Failed to publish stock arrival event");
        }
        return Optional.of(saved);
    }

    public List<ShippingRequest> getShippingRequests() {
        return databaseService.findItems("shipping", null, null, null).stream()
                .map(ShippingRequest.class::cast)
                .toList();
    }

    /* Controller endpoints moved from ShippingController into this service class */
    @PutMapping
    @Transactional
    public ShippingRequest confirmArrivalEndpoint(@RequestBody ShippingRequest request) {
        return confirmArrival(request).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "A shipping request is required"));
    }

    @GetMapping
    public List<ShippingRequest> getShippingRequestsEndpoint() {
        return getShippingRequests();
    }
}