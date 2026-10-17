package com.larp.shipping;

import com.larp.database.DatabaseService;
import com.larp.editor.InventoryItem;
import com.larp.events.StockEvent;
import com.larp.logviewer.LogService;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
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
import java.util.function.Consumer;

@Service
@RestController
@RequestMapping("/shipping")
public class ShippingService {

    private static final int UNINITIALIZED_STOCK_LEVEL = Integer.MIN_VALUE;

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

        InventoryItem incoming = new InventoryItem();
        incoming.setStatus("ARRIVED");
        incoming.setDate(request.getRequestedDate());
        if (databaseService.updateLatestItem("logistics", request.getItemCode(), incoming).isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unknown inventory item code: " + request.getItemCode());
        }

        request.setStatus("ARRIVED");
        ShippingRequest saved = ShippingRequest.class.cast(databaseService.createItem(request));
        logService.recordShippingConfirmation(request.getItemCode());
        int quantityDelta = request.getQuantity() == null ? 0 : request.getQuantity();
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

    @Bean
    public Consumer<KStream<String, StockEvent>> stockLevels() {
        return stream -> stream
                .groupByKey()
                .aggregate(
                        () -> UNINITIALIZED_STOCK_LEVEL,
                        (itemId, event, total) -> applyStockEvent(
                                total == UNINITIALIZED_STOCK_LEVEL ? getCurrentQuantity(itemId) : total,
                                event),
                        Materialized.as("stock-levels-store"))
                .toStream()
                .foreach((itemId, level) -> {
                    if (databaseService.updateLatestItem("logistics", itemId, quantityUpdate(level)).isEmpty()) {
                        throw new IllegalStateException(
                                "Database service did not update inventory for item " + itemId);
                    }
                });
    }

    private int getCurrentQuantity(String itemId) {
        return databaseService.findLatestItem("logistics", itemId)
                .map(InventoryItem.class::cast)
                .map(item -> item.getQuantity())
                .orElse(0);
    }

    private static InventoryItem quantityUpdate(int quantity) {
        InventoryItem update = new InventoryItem();
        update.setQuantity(quantity);
        update.setName(null);
        return update;
    }

    static int applyStockEvent(int total, StockEvent event) {
        return switch (event.type()) {
            case "STOCK_ARRIVED" -> total + event.quantityDelta();
            case "STOCK_DISPATCHED" -> total - event.quantityDelta();
            default -> total;
        };
    }

    /* Controller endpoints moved from ShippingController into this service class */
    @PutMapping
    public ShippingRequest confirmArrivalEndpoint(@RequestBody ShippingRequest request) {
        return confirmArrival(request).orElse(null);
    }

    @GetMapping
    public List<ShippingRequest> getShippingRequestsEndpoint() {
        return getShippingRequests();
    }
}