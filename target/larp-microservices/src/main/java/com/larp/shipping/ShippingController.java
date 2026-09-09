package com.larp.shipping;

import com.larp.logviewer.LogEntry;
import com.larp.logviewer.LogEntryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/shipping")
public class ShippingController {

    private final ShippingRequestRepository shippingRequestRepository;
    private final LogEntryRepository logEntryRepository;

    public ShippingController(ShippingRequestRepository shippingRequestRepository, LogEntryRepository logEntryRepository) {
        this.shippingRequestRepository = shippingRequestRepository;
        this.logEntryRepository = logEntryRepository;
    }

    @PutMapping
    public ResponseEntity<ShippingRequest> createShippingRequest(@RequestBody ShippingRequest request) {
        ShippingRequest saved = shippingRequestRepository.save(request);

        LogEntry logEntry = new LogEntry();
        logEntry.setMessage("Shipping request created: " + saved.getRequestId());
        logEntry.setLevel("INFO");
        logEntry.setTimestamp(LocalDateTime.now());
        logEntryRepository.save(logEntry);

        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public ResponseEntity<List<ShippingRequest>> getAllRequests() {
        return ResponseEntity.ok(shippingRequestRepository.findAll());
    }
}
