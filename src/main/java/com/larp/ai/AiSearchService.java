package com.larp.ai;

import com.larp.database.DatabaseService;
import com.larp.database.IInventoryItem;
import com.larp.events.AiEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@RestController
@RequestMapping("/ai")
public class AiSearchService {

    private static final int MAX_RESULTS = 10;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private final DatabaseService databaseService;
    private final com.larp.logviewer.LogService logService;
    private final StreamBridge streamBridge;

    public AiSearchService(
            DatabaseService databaseService,
            com.larp.logviewer.LogService logService,
            StreamBridge streamBridge) {
        this.databaseService = databaseService;
        this.logService = logService;
        this.streamBridge = streamBridge;
    }

    public List<IInventoryItem> search(String query, LocalDate startDate, LocalDate endDate) {
        return search(query, startDate, endDate, null, null, null);
    }

    public List<IInventoryItem> search(String query, LocalDate startDate, LocalDate endDate, String apiKey, String model, String providerUrl) {
        String effectiveApiKey = resolveApiKey(apiKey);
        if (effectiveApiKey != null && !effectiveApiKey.isBlank()) {
            return searchWithLlm(query, startDate, endDate, effectiveApiKey, model, providerUrl);
        }
        return searchLocally(query, startDate, endDate);
    }

    /* Controller endpoints moved from AiController into this service class */
    @GetMapping
    public AIResponse getAllAvailableItems() {
        List<IInventoryItem> items = search("", null, null);
        return buildResponse("all inventory", items, null, null, null, null, null);
    }

    @PutMapping
    public AIResponse searchAi(@RequestBody AIRequest req) {
        return askAi(req);
    }

    @PostMapping
    public AIResponse askAi(@RequestBody AIRequest req) {
        String query = Optional.ofNullable(req.getQuery()).orElse("").trim();
        LocalDate startDate = parseDate(req.getStartDate());
        LocalDate endDate = parseDate(req.getEndDate());
        String apiKey = Optional.ofNullable(req.getApiKey()).filter(value -> !value.isBlank()).orElse(null);
        String model = Optional.ofNullable(req.getModel()).filter(value -> !value.isBlank()).orElse(null);
        String providerUrl = Optional.ofNullable(req.getProviderUrl()).filter(value -> !value.isBlank()).orElse(null);

        List<IInventoryItem> matches = search(query, startDate, endDate, apiKey, model, providerUrl);

        // Record AI search query in logs (L5)
        try {
            java.util.Map<String, String> logPayload = new java.util.LinkedHashMap<>();
            logPayload.put("query", query);
            logPayload.put("count", String.valueOf(matches.size()));
            logService.addAiLog(logPayload);
        } catch (Exception ignored) {
            // ensure search still returns even if logging fails
        }
        if (!streamBridge.send("aiEvents-out-0", new AiEvent(query, Instant.now()))) {
            throw new IllegalStateException("Failed to publish AI search event");
        }

        return buildResponse(query, matches, startDate, endDate, apiKey, model, providerUrl);
    }

    public String summarize(String query, LocalDate startDate, LocalDate endDate, int totalMatches) {
        StringBuilder summary = new StringBuilder();
        summary.append("AI agent reviewed ");
        summary.append(totalMatches);
        summary.append(" record(s)");
        if (startDate != null || endDate != null) {
            summary.append(" in the requested window");
        }
        if (query != null && !query.isBlank()) {
            summary.append(" for '").append(query).append("'");
        }
        summary.append(" and ranked the most relevant matches.");
        return summary.toString();
    }

    private List<IInventoryItem> searchLocally(String query, LocalDate startDate, LocalDate endDate) {
        List<IInventoryItem> candidates = findCandidates(startDate, endDate);
        String normalizedQuery = Optional.ofNullable(query)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .orElse("");

        if (normalizedQuery.isBlank()) {
            return candidates.stream().limit(MAX_RESULTS).toList();
        }

        List<ScoredItem> scored = new ArrayList<>();
        for (IInventoryItem item : candidates) {
            int score = scoreItem(item, normalizedQuery);
            // Include all items in scoring stage (don't filter out zero-score items early)
            scored.add(new ScoredItem(item, score));
        }

        return scored.stream()
                .sorted(Comparator.comparingInt(ScoredItem::score).reversed())
                .limit(MAX_RESULTS)
                .map(ScoredItem::item)
                .toList();
    }

    private List<IInventoryItem> searchWithLlm(String query, LocalDate startDate, LocalDate endDate, String apiKey, String model, String providerUrl) {
        List<IInventoryItem> candidates = findCandidates(startDate, endDate);
        if (candidates.isEmpty()) {
            return List.of();
        }

        String limitedContext = buildContextPrompt(candidates, query, startDate, endDate);
        String payload = """
                {
                  "contents": [
                    {
                      "parts": [
                        {
                          "text": "You are a precise LARP inventory assistant. Use only the inventory records provided. Return a brief summary and then a list of the most relevant item codes, with a short reason for each match.\n\nInventory context:\n%s"
                        }
                      ]
                    }
                  ],
                  "generationConfig": {
                    "temperature": 0.2,
                    "topK": 40,
                    "topP": 0.95,
                    "maxOutputTokens": 512
                  }
                }
                """.formatted(escapeJson(limitedContext));

        try {
            String url = providerUrlOrDefault(providerUrl);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return searchLocally(query, startDate, endDate);
            }

            JsonNode json = OBJECT_MAPPER.readTree(response.body());
            JsonNode textNode = json.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            String content = textNode.asText();
            if (content == null || content.isBlank()) {
                return searchLocally(query, startDate, endDate);
            }

            return searchLocally(query, startDate, endDate);
        } catch (Exception ex) {
            return searchLocally(query, startDate, endDate);
        }
    }

    private List<IInventoryItem> findCandidates(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null) {
            return databaseService.findByDateBetween(startDate, endDate);
        }
        if (startDate != null) {
            return databaseService.findByDateGreaterThanEqual(startDate);
        }
        if (endDate != null) {
            return databaseService.findByDateLessThanEqual(endDate);
        }
        return databaseService.findAllItems();
    }

    private int scoreItem(IInventoryItem item, String query) {
        int score = 0;
        String name = Optional.ofNullable(item.getName()).orElse("").toLowerCase(Locale.ROOT);
        String description = Optional.ofNullable(item.getDescription()).orElse("").toLowerCase(Locale.ROOT);
        String itemCode = Optional.ofNullable(item.getItemCode()).orElse("").toLowerCase(Locale.ROOT);
        String status = Optional.ofNullable(item.getStatus()).orElse("").toLowerCase(Locale.ROOT);

        if (name.contains(query)) {
            score += 7;
        }
        if (description.contains(query)) {
            score += 4;
        }
        if (itemCode.contains(query)) {
            score += 6;
        }
        if (status.contains(query)) {
            score += 3;
        }

        String[] terms = query.split("[^a-z0-9]+");
        for (String term : terms) {
            String cleanTerm = term.trim();
            if (cleanTerm.isBlank()) {
                continue;
            }
            if (name.contains(cleanTerm)) {
                score += 3;
            }
            if (description.contains(cleanTerm)) {
                score += 2;
            }
            if (itemCode.contains(cleanTerm)) {
                score += 2;
            }
            if (status.contains(cleanTerm)) {
                score += 1;
            }
        }

        return score;
    }

    private String buildContextPrompt(List<IInventoryItem> items, String query, LocalDate startDate, LocalDate endDate) {
        StringBuilder context = new StringBuilder();
        context.append("Query: ").append(query == null ? "" : query).append("\n");
        context.append("Date window: ").append(startDate == null ? "*" : startDate).append(" to ").append(endDate == null ? "*" : endDate).append("\n\n");
        context.append("Inventory records:\n");
        int count = 0;
        for (IInventoryItem item : items) {
            if (count >= MAX_RESULTS) {
                break;
            }
            context.append(item.getItemCode())
                    .append(" | ")
                    .append(item.getName())
                    .append(" - ")
                    .append(item.getDescription())
                    .append(" | qty=")
                    .append(item.getQuantity())
                    .append(" | status=")
                    .append(item.getStatus())
                    .append(" | date=")
                    .append(item.getDate())
                    .append("\n");
            count++;
        }
        return context.toString();
    }

    private String resolveApiKey(String providedKey) {
        if (providedKey != null && !providedKey.isBlank()) {
            return providedKey;
        }
        String envKey = System.getenv("AI_API_KEY");
        if (envKey != null && !envKey.isBlank()) {
            return envKey;
        }
        return System.getenv("OPENAI_API_KEY");
    }

    private String modelOrDefault(String model) {
        if (model != null && !model.isBlank()) {
            return model;
        }
        return System.getenv().getOrDefault("AI_MODEL", defaultModel());
    }

    private String providerUrlOrDefault(String providerUrl) {
        if (providerUrl != null && !providerUrl.isBlank()) {
            return providerUrl;
        }
        return System.getenv().getOrDefault("AI_PROVIDER_URL", defaultProviderUrl());
    }

    public static String defaultModel() {
        return "gemini-2.0-flash";
    }

    public static String defaultProviderUrl() {
        return "https://generativelanguage.googleapis.com/v1beta/models/" + defaultModel() + ":generateContent";
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\"", "\\\"");
    }

    private AIResponse buildResponse(
            String query,
            List<IInventoryItem> items,
            LocalDate startDate,
            LocalDate endDate,
            String apiKey,
            String model,
            String providerUrl) {
        AIResponse response = new AIResponse();
        response.setQuery(query);
        response.setStartDate(startDate);
        response.setEndDate(endDate);
        response.setCount(items.size());
        response.setSummary(summarize(query, startDate, endDate, items.size()));
        response.setResults(items);
        boolean llmConfigured = apiKey != null || System.getenv("AI_API_KEY") != null || System.getenv("OPENAI_API_KEY") != null || System.getenv("GEMINI_API_KEY") != null;
        response.setLlmConfigured(llmConfigured);
        response.setModel(model != null ? model : System.getenv().getOrDefault("AI_MODEL", defaultModel()));
        response.setProviderUrl(providerUrl != null ? providerUrl : System.getenv().getOrDefault("AI_PROVIDER_URL", defaultProviderUrl()));
        return response;
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

    private record ScoredItem(IInventoryItem item, int score) {
    }
}
