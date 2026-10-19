package com.larp.ai;

import com.larp.database.DatabaseService;
import com.larp.editor.InventoryItem;
<<<<<<< HEAD
=======
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
>>>>>>> 3aee914b11d4fe54347f4e181b6413330c75b1b7
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

<<<<<<< HEAD
=======
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
>>>>>>> 3aee914b11d4fe54347f4e181b6413330c75b1b7
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Inventory search for the AI Assistant (A1). It is fast and deterministic, and it uses the terminology the agent
 * has learned, so a search for "blade" also finds swords and daggers once the agent has learned that.
 * The same search backs the agent's searchInventory tool. For questions that need reasoning, use POST /ai/chat.
 */
@Service
@RestController
@RequestMapping("/ai")
public class AiSearchService {

    public static final int MAX_RESULTS = 10;

    private final DatabaseService databaseService;
<<<<<<< HEAD
    private final StreamBridge streamBridge;
    private final TerminologyService terminology;

    public AiSearchService(DatabaseService databaseService, StreamBridge streamBridge, TerminologyService terminology) {
        this.databaseService = databaseService;
        this.streamBridge = streamBridge;
        this.terminology = terminology;
=======
    private final com.larp.logviewer.LogService logService;
    private final StreamBridge streamBridge;

    public AiSearchService(
            DatabaseService databaseService,
            com.larp.logviewer.LogService logService,
            StreamBridge streamBridge) {
        this.databaseService = databaseService;
        this.logService = logService;
        this.streamBridge = streamBridge;
>>>>>>> 3aee914b11d4fe54347f4e181b6413330c75b1b7
    }

    public List<InventoryItem> search(String query, LocalDate startDate, LocalDate endDate) {
        return search(query, startDate, endDate, MAX_RESULTS);
    }

    /**
     * Best matches first. Items that match none of the query's words (or their learned meanings) are left out.
     * A blank query returns the first {@code limit} items.
     */
    public List<InventoryItem> search(String query, LocalDate startDate, LocalDate endDate, int limit) {
        List<InventoryItem> candidates = databaseService.findItems("logistics", null, startDate, endDate).stream()
                .map(InventoryItem.class::cast)
                .toList();
        Set<String> words = terminology.expand(query);
        if (words.isEmpty()) {
            return candidates.stream().limit(limit).toList();
        }
<<<<<<< HEAD
        return candidates.stream()
                .map(item -> new ScoredItem(item, score(item, words)))
                .filter(scored -> scored.score() > 0)
=======
        return searchLocally(query, startDate, endDate);
    }

    /* Controller endpoints moved from AiController into this service class */
    @GetMapping
    public AIResponse getAllAvailableItems() {
        List<InventoryItem> items = search("", null, null);
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

        List<InventoryItem> matches = search(query, startDate, endDate, apiKey, model, providerUrl);

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

    private List<InventoryItem> searchLocally(String query, LocalDate startDate, LocalDate endDate) {
        List<InventoryItem> candidates = findCandidates(startDate, endDate);
        String normalizedQuery = Optional.ofNullable(query)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .orElse("");

        if (normalizedQuery.isBlank()) {
            return candidates.stream().limit(MAX_RESULTS).toList();
        }

        List<ScoredItem> scored = new ArrayList<>();
        for (InventoryItem item : candidates) {
            int score = scoreItem(item, normalizedQuery);
            // Include all items in scoring stage (don't filter out zero-score items early)
            scored.add(new ScoredItem(item, score));
        }

        return scored.stream()
>>>>>>> 3aee914b11d4fe54347f4e181b6413330c75b1b7
                .sorted(Comparator.comparingInt(ScoredItem::score).reversed())
                .limit(limit)
                .map(ScoredItem::item)
                .toList();
    }

    @GetMapping
    public AIResponse getAllAvailableItems() {
        List<InventoryItem> items = search("", null, null);
        return buildResponse("all inventory", items, null, null);
    }

<<<<<<< HEAD
    @PutMapping
    public AIResponse searchAi(@RequestBody AIRequest req) {
        return askAi(req);
=======
    private List<InventoryItem> findCandidates(LocalDate startDate, LocalDate endDate) {
        return databaseService.findItems("logistics", null, startDate, endDate).stream()
                .map(InventoryItem.class::cast)
                .toList();
>>>>>>> 3aee914b11d4fe54347f4e181b6413330c75b1b7
    }

    @PostMapping
    public AIResponse askAi(@RequestBody AIRequest req) {
        String query = Optional.ofNullable(req.getQuery()).orElse("").trim();
        LocalDate startDate = parseDate(req.getStartDate());
        LocalDate endDate = parseDate(req.getEndDate());

        List<InventoryItem> matches = search(query, startDate, endDate);

        // The Log Viewer records the search by consuming ai-events (L5).
        if (!streamBridge.send("aiEvents-out-0", new AiEvent(query, Instant.now()))) {
            throw new IllegalStateException("Failed to publish AI search event");
        }
        return buildResponse(query, matches, startDate, endDate);
    }

    static int score(InventoryItem item, Set<String> words) {
        String name = lower(item.getName());
        String description = lower(item.getDescription());
        String code = lower(item.getItemCode());
        String status = item.getStatus() == null ? "" : lower(item.getStatus().name());
        int score = 0;
<<<<<<< HEAD
        for (String word : words) {
            // "potions" should still find "potion"
            String stem = word.length() > 3 && word.endsWith("s") ? word.substring(0, word.length() - 1) : word;
            if (name.contains(stem)) {
                score += 3;
            }
            if (description.contains(stem)) {
=======
        String name = Optional.ofNullable(item.getName()).orElse("").toLowerCase(Locale.ROOT);
        String description = Optional.ofNullable(item.getDescription()).orElse("").toLowerCase(Locale.ROOT);
        String itemCode = Optional.ofNullable(item.getItemCode()).orElse("").toLowerCase(Locale.ROOT);
        String status = Optional.ofNullable(item.getStatus())
                .map(Enum::name)
                .orElse("")
                .toLowerCase(Locale.ROOT);

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
>>>>>>> 3aee914b11d4fe54347f4e181b6413330c75b1b7
                score += 1;
            }
            if (code.contains(stem)) {
                score += 3;
            }
            if (status.equals(word)) {
                score += 2;
            }
        }
        return score;
    }

    private AIResponse buildResponse(String query, List<InventoryItem> items, LocalDate startDate, LocalDate endDate) {
        List<String> searchTerms = List.copyOf(terminology.expand(query));
        AIResponse response = new AIResponse();
        response.setQuery(query);
        response.setStartDate(startDate);
        response.setEndDate(endDate);
        response.setCount(items.size());
        response.setSearchTerms(searchTerms);
        response.setResults(items);
        response.setSummary(summarize(query, searchTerms, startDate, endDate, items.size()));
        return response;
    }

    static String summarize(String query, List<String> searchTerms, LocalDate startDate, LocalDate endDate, int count) {
        StringBuilder summary = new StringBuilder("Found ").append(count).append(" item(s)");
        if (query != null && !query.isBlank()) {
            summary.append(" for '").append(query).append("'");
            List<String> queryWords = TerminologyService.splitWords(query);
            List<String> learned = searchTerms.stream().filter(term -> !queryWords.contains(term)).toList();
            if (!learned.isEmpty()) {
                summary.append(", also searching learned terms ").append(learned);
            }
<<<<<<< HEAD
=======
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
>>>>>>> 3aee914b11d4fe54347f4e181b6413330c75b1b7
        }
        if (startDate != null || endDate != null) {
            summary.append(" between ").append(startDate == null ? "*" : startDate)
                    .append(" and ").append(endDate == null ? "*" : endDate);
        }
        return summary.append(". Ask POST /ai/chat for a reasoned answer.").toString();
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
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

    private AIResponse buildResponse(
            String query,
            List<InventoryItem> items,
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

    private record ScoredItem(InventoryItem item, int score) {
    }
}
