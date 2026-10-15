package com.larp.shipping;

import com.larp.database.DatabaseService;
import com.larp.database.InventoryItem;
import com.larp.database.InventoryItem;
import com.larp.events.StockEvent;
import com.larp.logviewer.LogService;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

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

        List<InventoryItem> matchingItems = databaseService.findItems(request.getItemCode(), null, null); // TODO: Should be using generic IDatabaseItem implementations from database service.
        if (!matchingItems.isEmpty()) {
            InventoryItem incoming = new InventoryItem();
            incoming.setStatus("ARRIVED");
            if (request.getQuantity() != null) {
                incoming.setQuantity(request.getQuantity());
            }
            if (request.getRequestedDate() != null) {
                incoming.setDate(request.getRequestedDate());
            }
            databaseService.updateLatestItem(request.getItemCode(), incoming); // TODO: Should not be using a custom function from database service. All functions like this must be made in this entity or Should be using generic IDatabaseItem implementations from database service.
        }

        ShippingRequest saved = databaseService.createItem(request);
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
        return databaseService.findAllItems(ShippingRequest.class).stream()
                .map(ShippingRequest.class::cast)
                .toList();
    }

    @Bean
    public Consumer<KStream<String, StockEvent>> stockLevels(
            RestClient.Builder restClientBuilder,
            @Value("${larp.database.base-url:http://localhost:48484}") String databaseBaseUrl) {
        RestClient databaseRestClient = restClientBuilder.baseUrl(databaseBaseUrl).build();
        return stream -> stream
                .groupByKey()
                .aggregate(
                        () -> 0,
                        (itemId, event, total) -> applyStockEvent(total, event),
                        Materialized.as("stock-levels-store"))
                .toStream()
                .foreach((itemId, level) -> {
                    InventoryItem update = new InventoryItem();
                    update.setQuantity(level);
                    InventoryItem persisted = databaseRestClient.post()
                            .uri("/database/{itemId}", itemId)
                            .body(update)
                            .retrieve()
                            .body(InventoryItem.class);
                    if (persisted == null) {
                        throw new IllegalStateException(
                                "Database service did not update inventory for item " + itemId);
                    }
                });
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