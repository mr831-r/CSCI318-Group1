package com.larp.ai;

import com.larp.database.DatabaseService;
import com.larp.editor.InventoryItem;
import com.larp.shipping.ShippingRequest;
import com.larp.shipping.ShippingService;
import com.larp.shipping.ShippingStatus;
import com.larp.shipping.StockEvent;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * The autonomous side of the agent: it reacts to the existing "stock-events" Kafka topic without anyone asking.
 *
 * For every stock event (STOCK_ARRIVED, STOCK_DISPATCHED, and SHIPPING_REQUESTED when a camp asks for stock) it
 * 1. matches the new stock level against PENDING shipping requests for that item, oldest (most urgent) first
 *    - a deterministic check that always runs, and
 * 2. if an LLM is configured, asks the agent to review the situation with its tools. Any change the agent wants
 *    becomes a proposal in session "autonomous" that a person approves via /ai/actions.
 *
 * The result is written to the Log Viewer and the "agent-events" topic.
 * The consumer is bound in application.yml (agentStockEvents-in-0, consumer group "larp-agent").
 */
@Configuration
public class StockEventAgent {

    public static final String SESSION_ID = "autonomous";

    private static final Logger LOG = LoggerFactory.getLogger(StockEventAgent.class);

    private final DatabaseService databaseService;
    private final ShippingService shippingService;
    private final AgentRuntime runtime;
    private final AgentEventPublisher events;
    private final boolean llmReviewEnabled;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "larp-agent-autonomous");
        thread.setDaemon(true);
        return thread;
    });

    public StockEventAgent(
            DatabaseService databaseService,
            ShippingService shippingService,
            AgentRuntime runtime,
            AgentEventPublisher events,
            @Value("${larp.ai.autonomous-review:true}") boolean llmReviewEnabled) {
        this.databaseService = databaseService;
        this.shippingService = shippingService;
        this.runtime = runtime;
        this.events = events;
        this.llmReviewEnabled = llmReviewEnabled;
    }

    @Bean
    public Consumer<StockEvent> agentStockEvents() {
        // Hand off so the Kafka consumer thread is never blocked by a slow LLM call.
        return event -> worker.submit(() -> {
            try {
                handle(event);
            } catch (Exception ex) {
                LOG.warn("Autonomous stock review failed for {}: {}", event.itemId(), ex.getMessage());
            }
        });
    }

    /** Reviews one stock event. Returns the deterministic allocation summary (useful for tests). */
    public String handle(StockEvent event) {
        String summary = allocationSummary(event);
        events.publish("STOCK_REVIEW", SESSION_ID, summary);

        if (llmReviewEnabled && runtime.isConfigured()) {
            runtime.forget(SESSION_ID); // each review starts from a clean conversation
            String prompt = "Automatic trigger: Kafka event " + event.type() + " for item " + event.itemId()
                    + " (quantity change " + event.quantityDelta() + "). Rule-based check: " + summary
                    + "\nReview this item's stock, history and pending shipping requests with your tools. "
                    + "If something should change, use a propose... tool. Reply with a short recommendation.";
            runtime.chat(SESSION_ID, prompt)
                    .ifPresent(result -> events.publish("STOCK_RECOMMENDATION", SESSION_ID, result.content()));
        }
        return summary;
    }

    String allocationSummary(StockEvent event) {
        InventoryItem item = databaseService.findLatestItem("logistics", event.itemId())
                .map(InventoryItem.class::cast)
                .orElse(null);
        if (item == null) {
            return "Stock event for unknown item " + event.itemId();
        }
        // Same order as GET /shipping/requests: oldest requested date first, then oldest request.
        List<ShippingRequest> pending = shippingService.getRequestQueue(ShippingStatus.PENDING).stream()
                .filter(request -> event.itemId().equals(request.getItemCode()))
                .toList();

        int available = item.getQuantity() == null ? 0 : item.getQuantity();
        if (pending.isEmpty()) {
            return item.getItemCode() + " now has " + available + " in stock; no pending shipping requests.";
        }
        List<String> lines = new ArrayList<>();
        int remaining = available;
        for (ShippingRequest request : pending) {
            int wanted = request.getQuantity() == null ? 0 : request.getQuantity();
            if (wanted <= remaining) {
                remaining -= wanted;
                lines.add("can fill " + wanted + " for " + request.getDestination()
                        + " (requested " + request.getRequestedDate() + ")");
            } else {
                lines.add("short by " + (wanted - remaining) + " for " + request.getDestination()
                        + " (requested " + request.getRequestedDate() + ")");
                remaining = 0;
            }
        }
        return item.getItemCode() + " now has " + available + " in stock; " + pending.size()
                + " pending request(s), oldest first: " + String.join("; ", lines)
                + ". Left over after allocation: " + remaining + ".";
    }

    @PreDestroy
    void shutdown() {
        worker.shutdownNow();
    }
}
