package com.larp.editor;

import java.time.Instant;

public record ItemEvent(String type, String itemId, Instant occurredAt) {
}
