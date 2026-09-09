package com.larp.ai;

import com.larp.database.InventoryItem;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
public class AiController {

    private final AiSearchService aiSearchService;

    public AiController(AiSearchService aiSearchService) {
        this.aiSearchService = aiSearchService;
    }

    @GetMapping("/ai")
    public ResponseEntity<Map<String, Object>> getAllAvailableItems() {
        List<InventoryItem> items = aiSearchService.search("", null, null);
        return buildResponse("all inventory", items, null, null, null, null, null);
    }

    @PutMapping("/ai")
    public ResponseEntity<Map<String, Object>> searchAi(@RequestBody Map<String, String> payload) {
        return askAi(payload);
    }

    @PostMapping("/ai")
    public ResponseEntity<Map<String, Object>> askAi(@RequestBody Map<String, String> payload) {
        String query = Optional.ofNullable(payload.get("query")).orElse("").trim();
        LocalDate startDate = parseDate(payload.get("startDate"));
        LocalDate endDate = parseDate(payload.get("endDate"));
        String apiKey = Optional.ofNullable(payload.get("apiKey")).filter(value -> !value.isBlank()).orElse(null);
        String model = Optional.ofNullable(payload.get("model")).filter(value -> !value.isBlank()).orElse(null);
        String providerUrl = Optional.ofNullable(payload.get("providerUrl")).filter(value -> !value.isBlank()).orElse(null);

        List<InventoryItem> matches = aiSearchService.search(query, startDate, endDate, apiKey, model, providerUrl);
        return buildResponse(query, matches, startDate, endDate, apiKey, model, providerUrl);
    }

    private ResponseEntity<Map<String, Object>> buildResponse(
            String query,
            List<InventoryItem> items,
            LocalDate startDate,
            LocalDate endDate,
            String apiKey,
            String model,
            String providerUrl) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("query", query);
        response.put("startDate", startDate);
        response.put("endDate", endDate);
        response.put("count", items.size());
        response.put("summary", aiSearchService.summarize(query, startDate, endDate, items.size()));
        response.put("results", items);
        response.put("llmConfigured", apiKey != null || System.getenv("AI_API_KEY") != null || System.getenv("OPENAI_API_KEY") != null || System.getenv("GEMINI_API_KEY") != null);
        response.put("model", model != null ? model : System.getenv().getOrDefault("AI_MODEL", "gemini-2.0-flash"));
        response.put("providerUrl", providerUrl != null ? providerUrl : System.getenv().getOrDefault("AI_PROVIDER_URL", "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"));
        return ResponseEntity.ok(response);
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception ignored) {
            return null;
        }
    }
}
