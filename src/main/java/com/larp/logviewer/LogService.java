package com.larp.logviewer;

import com.larp.database.InventoryItem;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class LogService {

    private final LogEntryRepository logEntryRepository;

    public LogService(LogEntryRepository logEntryRepository) {
        this.logEntryRepository = logEntryRepository;
    }

    public List<LogEntry> findLogs(LocalDate start, LocalDate end) {
        if (start != null && end != null) {
            return logEntryRepository.findByTimestampBetween(start.atStartOfDay(), end.atTime(23, 59, 59));
        }

        return logEntryRepository.findAll();
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

    private LogEntry saveLog(String message, String level) {
        LogEntry logEntry = new LogEntry();
        logEntry.setMessage(message);
        logEntry.setLevel(level);
        logEntry.setTimestamp(LocalDateTime.now());
        return logEntryRepository.save(logEntry);
    }
}