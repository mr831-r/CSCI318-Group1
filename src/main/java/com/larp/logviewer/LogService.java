package com.larp.logviewer;

import com.larp.database.DatabaseService;
import com.larp.editor.InventoryItem;
import com.larp.events.AiEvent;
import com.larp.events.ItemEvent;
import com.larp.events.StockEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@RestController
@RequestMapping("/log")
public class LogService {

    private final DatabaseService databaseService;
    private final RestClient databaseRestClient;

    private final java.util.List<org.springframework.web.servlet.mvc.method.annotation.SseEmitter> emitters = new java.util.concurrent.CopyOnWriteArrayList<>();

    public LogService(
            DatabaseService databaseService,
            RestClient.Builder restClientBuilder,
            @Value("${larp.database.base-url:http://localhost:48484}") String databaseBaseUrl) {
        this.databaseService = databaseService;
        this.databaseRestClient = restClientBuilder.baseUrl(databaseBaseUrl).build();
    }

    public List<ILogEntry> findLogs(LocalDate start, LocalDate end) {
        return databaseService.findItems("log", null, start, end).stream()
                .map(LogEntry.class::cast)
                .sorted(Comparator.comparing(
                        LogEntry::getTimestamp,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(ILogEntry.class::cast)
                .collect(Collectors.toList());
    }

    public LogEntry addItemLog(InventoryItem item) {
        return saveLog("Inventory item action: " + (item != null ? item.getItemCode() : "unknown"), "INFO");
    }

    public LogEntry removeItemLog(InventoryItem item) {
        return saveLog("Inventory item removed: " + (item != null ? item.getItemCode() : "unknown"), "WARN");
    }

    public LogEntry addLogEntry(Map<String, String> payload) {
        return saveLog(payload.getOrDefault("message", "System event"), payload.getOrDefault("level", "INFO"));
    }

    public LogEntry addShippingLog(Map<String, String> payload) {
        return saveLog("Shipping event: " + payload.getOrDefault("itemCode", "unknown"), payload.getOrDefault("status", "INFO"));
    }

    public LogEntry addAiLog(Map<String, String> payload) {
        return saveLog("AI query: " + payload.getOrDefault("query", "general search"), "INFO");
    }

    public LogEntry recordShippingConfirmation(String itemCode) {
        return saveLog("Shipping confirmation received for item: " + itemCode, "INFO");
    }

    public LogEntry addLogItem(ItemEvent event) {
        return persistEventLog(
                event.type() + " for item " + event.itemId(),
                "INFO",
                event.occurredAt());
    }

    public LogEntry addLogItem(StockEvent event) {
        return persistEventLog(
                event.type() + " for item " + event.itemId() + ", quantity delta " + event.quantityDelta(),
                "INFO",
                event.occurredAt());
    }

    public LogEntry addLogItem(AiEvent event) {
        return persistEventLog("AI query: " + event.query(), "INFO", event.occurredAt());
    }

    @Bean
    public Consumer<ItemEvent> logItemEvents() {
        return this::addLogItem;
    }

    @Bean
    public Consumer<AiEvent> logAiEvents() {
        return this::addLogItem;
    }

    @Bean
    public Consumer<StockEvent> logStockEvents() {
        return this::addLogItem;
    }

    private LogEntry saveLog(String message, String level) {
        LogEntry logEntry = new LogEntry();
        logEntry.setMessage(message);
        logEntry.setLevel(level);
        logEntry.setTimestamp(LocalDateTime.now());

        // Display the log on the command prompt for the user (per L3)
        try {
            System.out.println("[" + logEntry.getTimestamp() + "] " + level + ": " + message);
        } catch (Exception ignore) {
            // If printing fails for any reason, continue to persist the log
        }

        LogEntry persisted = LogEntry.class.cast(databaseService.createItem(logEntry));

        // Notify SSE subscribers of the new log entry
        notifyEmitters(persisted);

        return persisted;
    }

    private LogEntry persistEventLog(String message, String level, Instant occurredAt) {
        LogEntry entry = new LogEntry();
        entry.setMessage(message);
        entry.setLevel(level);
        entry.setTimestamp(LocalDateTime.ofInstant(occurredAt, ZoneId.systemDefault()));

        LogEntry persisted = databaseRestClient.put()
                .uri("/database/logs")
                .body(entry)
                .retrieve()
                .body(LogEntry.class);
        if (persisted == null) {
            throw new IllegalStateException("Database service returned no persisted log entry");
        }
        notifyEmitters(persisted);
        return persisted;
    }

    private void notifyEmitters(LogEntry entry) {
        for (org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter : emitters) {
            try {
                org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder event = org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                        .name("log")
                        .data(entry)
                        .id(String.valueOf(entry.getId()))
                        .reconnectTime(3000L);
                emitter.send(event);
            } catch (Exception ex) {
                // remove dead emitters
                emitters.remove(emitter);
            }
        }
    }

    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter subscribeToLogs() {
        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter = new org.springframework.web.servlet.mvc.method.annotation.SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((ex) -> emitters.remove(emitter));
        return emitter;
    }

    /* Controller endpoints moved from LogViewerController into this service class */

    @GetMapping
    public List<ILogEntry> getLogs(@RequestParam(value = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                   @RequestParam(value = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return findLogs(start, end);
    }

    @PutMapping("/item")
    public LogEntry addItemLogEndpoint(@RequestBody InventoryItem item) {
        return addItemLog(item);
    }

    @DeleteMapping("/item")
    public LogEntry removeItemLogEndpoint(@RequestBody InventoryItem item) {
        return removeItemLog(item);
    }

    @PutMapping
    public LogEntry addLogEntryEndpoint(@RequestBody Map<String, String> payload) {
        return addLogEntry(payload);
    }

    @PutMapping("/shipping")
    public LogEntry addShippingLogEndpoint(@RequestBody Map<String, String> payload) {
        return addShippingLog(payload);
    }

    @PutMapping("/ai")
    public LogEntry addAiLogEndpoint(@RequestBody Map<String, String> payload) {
        return addAiLog(payload);
    }

    @GetMapping("/stream")
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamLogs() {
        return subscribeToLogs();
    }
}