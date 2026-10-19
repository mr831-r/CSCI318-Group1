package com.larp.ai;

import java.time.Instant;

/**
 * Audit event published to the Kafka topic "agent-events" (consumed by the Log Viewer) whenever the agent answers, proposes,
 * or a person approves/rejects one of its proposals.
 *
 * @param type      CHAT, ACTION_PROPOSED, ACTION_APPROVED, ACTION_REJECTED, ACTION_FAILED, TERM_LEARNED, STOCK_REVIEW or STOCK_RECOMMENDATION
 * @param sessionId the conversation the event belongs to ("autonomous" for event-driven reviews)
 * @param detail    short description of what happened
 */
public record AgentEvent(String type, String sessionId, String detail, Instant occurredAt) {
}
