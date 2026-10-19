package com.larp.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Makes the agent auditable: every agent decision is published to the "agent-events" Kafka topic, which the
 * Log Viewer consumes (LogService.logAgentEvents) like the other event topics, so it appears in GET /log and
 * the /log/stream feed with level AGENT.
 *
 * Failures here never break an agent reply - auditing is best effort.
 */
@Component
public class AgentEventPublisher {

    public static final String BINDING = "agentEvents-out-0";
    static final int DETAIL_LIMIT = 900;

    private static final Logger LOG = LoggerFactory.getLogger(AgentEventPublisher.class);

    private final StreamBridge streamBridge;

    public AgentEventPublisher(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    public void publish(String type, String sessionId, String detail) {
        AgentEvent event = new AgentEvent(type, sessionId, truncate(detail, DETAIL_LIMIT), Instant.now());
        try {
            if (!streamBridge.send(BINDING, event)) {
                LOG.warn("Agent event was not accepted by Kafka: {}", type);
            }
        } catch (Exception ex) {
            LOG.warn("Could not publish agent event to Kafka: {}", ex.getMessage());
        }
    }

    static String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max - 3) + "...";
    }
}
