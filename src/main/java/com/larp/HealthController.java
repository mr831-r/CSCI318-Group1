package com.larp;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("service", "larp-microservices");
        response.put("endpoints", new String[]{
                "/database",
                "/editor",
                "/editor/statuses",
                "/log",
                "/shipping",
                "/shipping/requests",
                "/ai",
                "/ai/chat",
                "/ai/actions",
                "/console",
                "/h2-console"
        });
        return ResponseEntity.ok(response);
    }
}
