package com.larp.logviewer;

import com.larp.database.DatabaseService;
import com.larp.database.InventoryItem;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RestController
@RequestMapping("/log")
public class LogService {

    private final DatabaseService databaseService;

    public LogService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    public List<LogEntry> findLogs(LocalDate start, LocalDate end) {
        return databaseService.findLogs(start, end);
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

        // Display the log on the command prompt for the user (per L3)
        try {
            System.out.println("[" + logEntry.getTimestamp() + "] " + level + ": " + message);
        } catch (Exception ignore) {
            // If printing fails for any reason, continue to persist the log
        }

        return databaseService.saveLogEntry(logEntry);
    }

    /* Controller endpoints moved from LogViewerController into this service class */

    @GetMapping
    public List<LogEntry> getLogs(@RequestParam(value = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
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
}