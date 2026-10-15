package com.larp.events;

import java.time.Instant;

public record AiEvent(String query, Instant occurredAt) {
}
