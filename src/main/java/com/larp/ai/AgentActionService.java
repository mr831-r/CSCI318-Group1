package com.larp.ai;

import com.larp.database.DatabaseService;
import com.larp.editor.EditorService;
import com.larp.editor.InventoryItem;
import com.larp.editor.InventoryStatus;
import com.larp.shipping.ShippingRequest;
import com.larp.shipping.ShippingService;
import com.larp.shipping.ShippingStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Human-in-the-loop guard rail. The agent's write tools only create proposals here; nothing changes in the
 * database until a person approves the proposal. Approved proposals run through the existing services
 * (EditorService / ShippingService), so their validation, logging and Kafka events still apply.
 */
@Service
public class AgentActionService {

    private final Map<String, PendingAction> actions = new ConcurrentHashMap<>();

    private final DatabaseService databaseService;
    private final EditorService editorService;
    private final ShippingService shippingService;
    private final AgentEventPublisher events;

    public AgentActionService(
            DatabaseService databaseService,
            EditorService editorService,
            ShippingService shippingService,
            AgentEventPublisher events) {
        this.databaseService = databaseService;
        this.editorService = editorService;
        this.shippingService = shippingService;
        this.events = events;
    }

    public PendingAction proposeQuantity(String sessionId, String itemCode, int quantity, String reason) {
        requireItem(itemCode);
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity must be zero or more");
        }
        return store(sessionId, PendingAction.Type.SET_QUANTITY, itemCode, quantity, null, null, null, null, reason);
    }

    public PendingAction proposeStatus(String sessionId, String itemCode, String status, String reason) {
        requireItem(itemCode);
        InventoryStatus parsed = InventoryStatus.fromValue(status); // throws for unknown values
        return store(sessionId, PendingAction.Type.SET_STATUS, itemCode, null, parsed.name(), null, null, null, reason);
    }

    public PendingAction proposeArrival(String sessionId, String itemCode, int quantity, String destination, String reason) {
        requireItem(itemCode);
        if (quantity <= 0) {
            throw new IllegalArgumentException("Arrival quantity must be more than zero");
        }
        if (destination == null || destination.isBlank()) {
            throw new IllegalArgumentException("A destination is required");
        }
        return store(sessionId, PendingAction.Type.CONFIRM_ARRIVAL, itemCode, quantity, null, destination.trim(),
                null, null, reason);
    }

    public PendingAction proposeDispatch(String sessionId, long requestId, String courier, String reason) {
        if (courier == null || courier.isBlank()) {
            throw new IllegalArgumentException("A courier is required");
        }
        ShippingRequest request = databaseService.findItem("shipping", requestId)
                .map(ShippingRequest.class::cast)
                .orElseThrow(() -> new IllegalArgumentException("Unknown shipping request: " + requestId));
        if (request.getStatus() != ShippingStatus.PENDING) {
            throw new IllegalArgumentException("Shipping request " + requestId + " is " + request.getStatus()
                    + ", only PENDING requests can be dispatched");
        }
        int inStock = Optional.ofNullable(requireItem(request.getItemCode()).getQuantity()).orElse(0);
        if (inStock < request.getQuantity()) {
            throw new IllegalArgumentException("Not enough stock to dispatch request " + requestId + ": "
                    + inStock + " x " + request.getItemCode() + " available, " + request.getQuantity() + " requested");
        }
        return store(sessionId, PendingAction.Type.DISPATCH_REQUEST, request.getItemCode(), request.getQuantity(),
                null, request.getDestination(), requestId, courier.trim(), reason);
    }

    /** All proposals, newest first. Pass a session id to only see that conversation's proposals. */
    public List<PendingAction> list(String sessionId) {
        return actions.values().stream()
                .filter(action -> sessionId == null || sessionId.isBlank() || sessionId.equals(action.sessionId()))
                .sorted(Comparator.comparing(PendingAction::createdAt).reversed())
                .toList();
    }

    public List<PendingAction> pending(String sessionId) {
        return list(sessionId).stream()
                .filter(action -> action.state() == PendingAction.State.PENDING)
                .toList();
    }

    public Optional<PendingAction> find(String id) {
        return Optional.ofNullable(actions.get(id));
    }

    /** Runs the proposal through the existing services. */
    public synchronized PendingAction approve(String id) {
        PendingAction action = requirePending(id);
        PendingAction resolved;
        try {
            String outcome = execute(action);
            resolved = action.resolve(PendingAction.State.APPROVED, outcome);
            events.publish("ACTION_APPROVED", action.sessionId(), action.describe() + " -> " + outcome);
            supersedeDuplicates(action);
        } catch (RuntimeException ex) {
            String reason = ex instanceof ResponseStatusException status && status.getReason() != null
                    ? status.getReason() : ex.getMessage();
            resolved = action.resolve(PendingAction.State.FAILED, reason);
            events.publish("ACTION_FAILED", action.sessionId(), action.describe() + " -> " + reason);
        }
        actions.put(id, resolved);
        return resolved;
    }

    public synchronized PendingAction reject(String id) {
        PendingAction action = requirePending(id);
        PendingAction resolved = action.resolve(PendingAction.State.REJECTED, "Rejected by user");
        actions.put(id, resolved);
        events.publish("ACTION_REJECTED", action.sessionId(), action.describe());
        return resolved;
    }

    private String execute(PendingAction action) {
        return switch (action.type()) {
            case SET_QUANTITY -> {
                InventoryItem item = requireItem(action.itemCode());
                item.setQuantity(action.quantity());
                InventoryItem saved = editorService.validateAndSave(item);
                yield "Quantity of " + saved.getItemCode() + " is now " + saved.getQuantity();
            }
            case SET_STATUS -> {
                InventoryItem item = requireItem(action.itemCode());
                item.setStatus(InventoryStatus.fromValue(action.status()));
                InventoryItem saved = editorService.validateAndSave(item);
                yield "Status of " + saved.getItemCode() + " is now " + saved.getStatus();
            }
            case CONFIRM_ARRIVAL -> {
                ShippingRequest request = new ShippingRequest();
                request.setItemCode(action.itemCode());
                request.setQuantity(action.quantity());
                request.setDestination(action.destination());
                request.setRequestedDate(LocalDate.now());
                ShippingRequest saved = shippingService.confirmArrival(request)
                        .orElseThrow(() -> new IllegalStateException("Shipping service rejected the arrival"));
                yield "Arrival recorded for " + saved.getQuantity() + " x " + saved.getItemCode();
            }
            case DISPATCH_REQUEST -> {
                ShippingRequest dispatched = shippingService.dispatch(action.requestId(), action.courier());
                yield "Request " + dispatched.getId() + " dispatched with " + dispatched.getCourier();
            }
        };
    }

    /** Once a change is approved, other open proposals for the same change are no longer needed. */
    private void supersedeDuplicates(PendingAction approved) {
        actions.values().stream()
                .filter(other -> other.state() == PendingAction.State.PENDING)
                .filter(other -> !other.id().equals(approved.id()))
                .filter(other -> sameTarget(other, approved))
                .forEach(other -> actions.put(other.id(), other.resolve(PendingAction.State.REJECTED,
                        "Superseded by approved action " + approved.id())));
    }

    private static boolean sameTarget(PendingAction a, PendingAction b) {
        if (a.type() != b.type()) {
            return false;
        }
        return a.type() == PendingAction.Type.DISPATCH_REQUEST
                ? Objects.equals(a.requestId(), b.requestId())
                : a.type() != PendingAction.Type.CONFIRM_ARRIVAL && Objects.equals(a.itemCode(), b.itemCode());
    }

    private PendingAction store(String sessionId, PendingAction.Type type, String itemCode, Integer quantity,
                                String status, String destination, Long requestId, String courier, String reason) {
        // The automatic reviewer runs on every stock event; don't stack up identical open proposals.
        Optional<PendingAction> duplicate = actions.values().stream()
                .filter(open -> open.state() == PendingAction.State.PENDING && open.type() == type)
                .filter(open -> Objects.equals(open.itemCode(), itemCode.trim()) && Objects.equals(open.quantity(), quantity)
                        && Objects.equals(open.status(), status) && Objects.equals(open.destination(), destination)
                        && Objects.equals(open.requestId(), requestId) && Objects.equals(open.courier(), courier))
                .findFirst();
        if (duplicate.isPresent()) {
            return duplicate.get();
        }
        String id = UUID.randomUUID().toString().substring(0, 8);
        PendingAction action = new PendingAction(id, sessionId, type, itemCode.trim(), quantity, status,
                destination, requestId, courier, reason, PendingAction.State.PENDING, null, Instant.now());
        actions.put(id, action);
        events.publish("ACTION_PROPOSED", sessionId, action.describe() + " (id " + id + "): " + reason);
        return action;
    }

    private PendingAction requirePending(String id) {
        PendingAction action = actions.get(id);
        if (action == null) {
            throw new IllegalArgumentException("Unknown action id: " + id);
        }
        if (action.state() != PendingAction.State.PENDING) {
            throw new IllegalStateException("Action " + id + " is already " + action.state());
        }
        return action;
    }

    private InventoryItem requireItem(String itemCode) {
        if (itemCode == null || itemCode.isBlank()) {
            throw new IllegalArgumentException("An item code is required");
        }
        return databaseService.findLatestItem("logistics", itemCode.trim())
                .map(InventoryItem.class::cast)
                .orElseThrow(() -> new IllegalArgumentException("Unknown inventory item code: " + itemCode));
    }
}
