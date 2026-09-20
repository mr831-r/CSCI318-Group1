package com.larp.logviewer;

import com.larp.database.InventoryItem;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/log")
public class LogViewerController {

    private final LogService logService;

    public LogViewerController(LogService logService) {
        this.logService = logService;
    }

    @GetMapping
    public ResponseEntity<List<LogEntry>> getLogs(
            @RequestParam(value = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(value = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        return ResponseEntity.ok(logService.findLogs(start, end));
    }

    @PutMapping("/item")
    public ResponseEntity<LogEntry> addItemLog(@RequestBody InventoryItem item) {
        return ResponseEntity.ok(logService.addItemLog(item));
    }

    @DeleteMapping("/item")
    public ResponseEntity<LogEntry> removeItemLog(@RequestBody InventoryItem item) {
        return ResponseEntity.ok(logService.removeItemLog(item));
    }

    @PutMapping
    public ResponseEntity<LogEntry> addLogEntry(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(logService.addLogEntry(payload));
    }

    @PutMapping("/shipping")
    public ResponseEntity<LogEntry> addShippingLog(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(logService.addShippingLog(payload));
    }

    @PutMapping("/ai")
    public ResponseEntity<LogEntry> addAiLog(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(logService.addAiLog(payload));
    }
}
