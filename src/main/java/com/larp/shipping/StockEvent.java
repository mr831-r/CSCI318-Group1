package com.larp.shipping;

import java.time.Instant;

public record StockEvent(String type, String itemId, int quantityDelta, Instant occurredAt) {
}
