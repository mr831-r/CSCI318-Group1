package com.larp.ai;

import java.time.Instant;

public record AiEvent(String query, Instant occurredAt) {
}
