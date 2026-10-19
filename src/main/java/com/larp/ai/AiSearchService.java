package com.larp.ai;

import com.larp.database.DatabaseService;
import com.larp.editor.InventoryItem;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    private final StreamBridge streamBridge;
    private final TerminologyService terminology;

    public AiSearchService(DatabaseService databaseService, StreamBridge streamBridge, TerminologyService terminology) {
        this.databaseService = databaseService;
        this.streamBridge = streamBridge;
        this.terminology = terminology;
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
        return candidates.stream()
                .map(item -> new ScoredItem(item, score(item, words)))
                .filter(scored -> scored.score() > 0)
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

    @PutMapping
    public AIResponse searchAi(@RequestBody AIRequest req) {
        return askAi(req);
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
        for (String word : words) {
            // "potions" should still find "potion"
            String stem = word.length() > 3 && word.endsWith("s") ? word.substring(0, word.length() - 1) : word;
            if (name.contains(stem)) {
                score += 3;
            }
            if (description.contains(stem)) {
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

    private record ScoredItem(InventoryItem item, int score) {
    }
}
