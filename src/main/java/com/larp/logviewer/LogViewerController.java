package com.larp.logviewer;

import com.larp.database.InventoryItem;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/log")
public class LogViewerController {

    private final LogEntryRepository logEntryRepository;

    public LogViewerController(LogEntryRepository logEntryRepository) {
        this.logEntryRepository = logEntryRepository;
    }

    @GetMapping
    public ResponseEntity<List<LogEntry>> getLogs(
            @RequestParam(value = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(value = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        if (start != null && end != null) {
            LocalDateTime startDateTime = start.atStartOfDay();
            LocalDateTime endDateTime = end.atTime(23, 59, 59);
            return ResponseEntity.ok(logEntryRepository.findByTimestampBetween(startDateTime, endDateTime));
        }

        return ResponseEntity.ok(logEntryRepository.findAll());
    }

    @PutMapping("/item")
    public ResponseEntity<LogEntry> addItemLog(@RequestBody InventoryItem item) {
        LogEntry logEntry = new LogEntry();
        logEntry.setMessage("Inventory item action: " + (item != null ? item.getItemCode() : "unknown"));
        logEntry.setLevel("INFO");
        logEntry.setTimestamp(LocalDateTime.now());
        return ResponseEntity.ok(logEntryRepository.save(logEntry));
    }

    @DeleteMapping("/item")
    public ResponseEntity<LogEntry> removeItemLog(@RequestBody InventoryItem item) {
        LogEntry logEntry = new LogEntry();
        logEntry.setMessage("Inventory item removed: " + (item != null ? item.getItemCode() : "unknown"));
        logEntry.setLevel("WARN");
        logEntry.setTimestamp(LocalDateTime.now());
        return ResponseEntity.ok(logEntryRepository.save(logEntry));
    }

    @PutMapping
    public ResponseEntity<LogEntry> addLogEntry(@RequestBody Map<String, String> payload) {
        LogEntry logEntry = new LogEntry();
        logEntry.setMessage(payload.getOrDefault("message", "System event"));
        logEntry.setLevel(payload.getOrDefault("level", "INFO"));
        logEntry.setTimestamp(LocalDateTime.now());
        return ResponseEntity.ok(logEntryRepository.save(logEntry));
    }

    @PutMapping("/shipping")
    public ResponseEntity<LogEntry> addShippingLog(@RequestBody Map<String, String> payload) {
        LogEntry logEntry = new LogEntry();
        logEntry.setMessage("Shipping event: " + payload.getOrDefault("itemCode", "unknown"));
        logEntry.setLevel(payload.getOrDefault("status", "INFO"));
        logEntry.setTimestamp(LocalDateTime.now());
        return ResponseEntity.ok(logEntryRepository.save(logEntry));
    }

    @PutMapping("/ai")
    public ResponseEntity<LogEntry> addAiLog(@RequestBody Map<String, String> payload) {
        LogEntry logEntry = new LogEntry();
        logEntry.setMessage("AI query: " + payload.getOrDefault("query", "general search"));
        logEntry.setLevel("INFO");
        logEntry.setTimestamp(LocalDateTime.now());
        return ResponseEntity.ok(logEntryRepository.save(logEntry));
    }
}
