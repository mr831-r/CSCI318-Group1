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
<<<<<<< HEAD
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
=======
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
>>>>>>> 3aee914b11d4fe54347f4e181b6413330c75b1b7
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
<<<<<<< HEAD
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
=======
import java.util.List;
>>>>>>> 3aee914b11d4fe54347f4e181b6413330c75b1b7
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
<<<<<<< HEAD
        publishStockEvent("STOCK_ARRIVED", request.getItemCode(), request.getQuantity());
        return Optional.of(saved);
    }

    /**
     * Records a camp's request for stock (status PENDING). Publishes SHIPPING_REQUESTED so stock can be matched
     * to the new request straight away.
     */
    @Transactional
    public ShippingRequest createRequest(ShippingRequest request) {
        if (request == null || request.getItemCode() == null || request.getItemCode().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An item code is required");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requested quantity must be more than zero");
        }
        if (request.getDestination() == null || request.getDestination().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A destination is required");
        }
        if (databaseService.findLatestItem("logistics", request.getItemCode()).isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Unknown inventory item code: " + request.getItemCode());
        }
        request.setId(null);
        request.setStatus(ShippingStatus.PENDING);
        request.setCourier(null);
        if (request.getRequestedDate() == null) {
            request.setRequestedDate(LocalDate.now());
        }
        ShippingRequest saved = ShippingRequest.class.cast(databaseService.createItem(request));
        publishStockEvent("SHIPPING_REQUESTED", saved.getItemCode(), 0);
        return saved;
    }

    /**
     * Sends a pending request: takes the quantity out of stock, assigns the courier and marks the request
     * DISPATCHED. Fails with 409 if there is not enough stock. Publishes STOCK_DISPATCHED (negative delta).
     */
    @Transactional
    public ShippingRequest dispatch(Long requestId, String courier) {
        if (courier == null || courier.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A courier is required");
        }
        ShippingRequest request = databaseService.findItem("shipping", requestId)
                .map(ShippingRequest.class::cast)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Unknown shipping request: " + requestId));
        if (request.getStatus() != ShippingStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Shipping request " + requestId + " is " + request.getStatus() + ", not PENDING");
        }
        int quantity = request.getQuantity();
        databaseService.mutateLatestItem("logistics", request.getItemCode(), item -> {
            InventoryItem inventoryItem = (InventoryItem) item;
            if (inventoryItem.getQuantity() < quantity) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough stock of " + request.getItemCode()
                        + ": " + inventoryItem.getQuantity() + " available, " + quantity + " requested");
            }
            inventoryItem.setQuantity(inventoryItem.getQuantity() - quantity);
        }).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Unknown inventory item code: " + request.getItemCode()));
        ShippingRequest dispatched = databaseService.mutateItem("shipping", requestId, item -> {
                    ShippingRequest shipping = (ShippingRequest) item;
                    shipping.setStatus(ShippingStatus.DISPATCHED);
                    shipping.setCourier(courier.trim());
                })
                .map(ShippingRequest.class::cast)
                .orElseThrow();
        publishStockEvent("STOCK_DISPATCHED", dispatched.getItemCode(), -quantity);
        return dispatched;
    }

    /** Requests with the given status (all non-arrival requests when null), oldest requested date first. */
    public List<ShippingRequest> getRequestQueue(ShippingStatus status) {
        return getShippingRequests().stream()
                .filter(request -> status == null
                        ? request.getStatus() != ShippingStatus.ARRIVED
                        : request.getStatus() == status)
                .sorted(Comparator.comparing(ShippingRequest::getRequestedDate,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ShippingRequest::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private void publishStockEvent(String type, String itemCode, int quantityDelta) {
        StockEvent event = new StockEvent(type, itemCode, quantityDelta, Instant.now());
=======
        int quantityDelta = request.getQuantity();
        StockEvent event = new StockEvent(
                "STOCK_ARRIVED",
                request.getItemCode(),
                quantityDelta,
                Instant.now());
>>>>>>> 3aee914b11d4fe54347f4e181b6413330c75b1b7
        boolean published = streamBridge.send(
                "stockEvents-out-0",
                MessageBuilder.withPayload(event)
                        .setHeader(KafkaHeaders.KEY, event.itemId())
                        .build());
        if (!published) {
<<<<<<< HEAD
            throw new IllegalStateException("Failed to publish stock event " + type);
        }
=======
            throw new IllegalStateException("Failed to publish stock arrival event");
        }
        return Optional.of(saved);
>>>>>>> 3aee914b11d4fe54347f4e181b6413330c75b1b7
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
<<<<<<< HEAD

    @PutMapping("/requests")
    public ShippingRequest createRequestEndpoint(@RequestBody ShippingRequest request) {
        return createRequest(request);
    }

    @GetMapping("/requests")
    public List<ShippingRequest> getRequestQueueEndpoint(@RequestParam(value = "status", required = false) String status) {
        return getRequestQueue(status == null || status.isBlank() ? null : ShippingStatus.fromValue(status));
    }

    @PostMapping("/requests/{id}/dispatch")
    public ShippingRequest dispatchEndpoint(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return dispatch(id, body == null ? null : body.get("courier"));
    }
=======
>>>>>>> 3aee914b11d4fe54347f4e181b6413330c75b1b7
}