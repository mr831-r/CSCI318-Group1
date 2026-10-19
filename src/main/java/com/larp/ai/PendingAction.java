package com.larp.ai;

import java.time.Instant;

/**
 * A change the agent wants to make to stock. The agent can only propose it; a person approves or rejects it
 * through POST /ai/actions/{id}/approve or /reject (human-in-the-loop).
 */
public record PendingAction(
        String id,
        String sessionId,
        Type type,
        String itemCode,
        Integer quantity,
        String status,
        String destination,
        Long requestId,
        String courier,
        String reason,
        State state,
        String outcome,
        Instant createdAt) {

    public enum Type {
        /** Set an inventory item's quantity to an absolute value (EditorService.validateAndSave). */
        SET_QUANTITY,
        /** Change an inventory item's status (EditorService.validateAndSave). */
        SET_STATUS,
        /** Record received stock (ShippingService.confirmArrival). */
        CONFIRM_ARRIVAL,
        /** Send a pending shipping request with a courier (ShippingService.dispatch). */
        DISPATCH_REQUEST
    }

    public enum State {
        PENDING,
        APPROVED,
        REJECTED,
        FAILED
    }

    public PendingAction resolve(State newState, String newOutcome) {
        return new PendingAction(id, sessionId, type, itemCode, quantity, status, destination, requestId, courier,
                reason, newState, newOutcome, createdAt);
    }

    /** Short human readable description, also shown to the model. */
    public String describe() {
        return switch (type) {
            case SET_QUANTITY -> "Set quantity of " + itemCode + " to " + quantity;
            case SET_STATUS -> "Set status of " + itemCode + " to " + status;
            case CONFIRM_ARRIVAL -> "Confirm arrival of " + quantity + " x " + itemCode + " at " + destination;
            case DISPATCH_REQUEST -> "Dispatch request " + requestId + " (" + quantity + " x " + itemCode + " to "
                    + destination + ") with courier " + courier;
        };
    }
}
