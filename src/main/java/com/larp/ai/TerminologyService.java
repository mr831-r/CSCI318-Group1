package com.larp.ai;

import com.larp.database.DatabaseService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Learns and applies item terminology so later searches understand words that do not appear in item names.
 */
@Service
public class TerminologyService {

    private final DatabaseService databaseService;

    public TerminologyService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    /** Saves (or replaces) the meaning of a term. */
    public TermAlias learn(String term, String meaning, String learnedFrom) {
        String normalizedTerm = normalize(term);
        String normalizedMeaning = normalize(meaning);
        if (normalizedTerm.isEmpty() || normalizedMeaning.isEmpty()) {
            throw new IllegalArgumentException("Both a term and its meaning are required");
        }
        Optional<TermAlias> existing = find(normalizedTerm);
        if (existing.isPresent()) {
            TermAlias update = new TermAlias(normalizedTerm, normalizedMeaning, LocalDate.now(), learnedFrom);
            return databaseService.updateLatestItem(TermAlias.FILTER_NAME, normalizedTerm, update)
                    .map(TermAlias.class::cast)
                    .orElseThrow();
        }
        TermAlias alias = new TermAlias(normalizedTerm, normalizedMeaning, LocalDate.now(), learnedFrom);
        return TermAlias.class.cast(databaseService.createItem(alias));
    }

    public List<TermAlias> all() {
        return databaseService.findItems(TermAlias.FILTER_NAME, null, null, null).stream()
                .map(TermAlias.class::cast)
                .toList();
    }

    public Optional<TermAlias> find(String term) {
        return databaseService.findLatestItem(TermAlias.FILTER_NAME, normalize(term))
                .map(TermAlias.class::cast);
    }

    /**
     * Returns the search words for a query: the query's own words plus the meanings of any learned terms.
     * Example: with "blade" learned as "sword dagger", the query "steel blade" gives [steel, blade, sword, dagger].
     */
    public Set<String> expand(String query) {
        Set<String> words = new LinkedHashSet<>(splitWords(query));
        if (words.isEmpty()) {
            return words;
        }
        for (TermAlias alias : all()) {
            String term = alias.getTerm();
            boolean matches = words.contains(term) || normalize(query).contains(term);
            if (matches) {
                words.addAll(splitWords(alias.getMeaning()));
            }
        }
        return words;
    }

    static List<String> splitWords(String text) {
        return Arrays.stream(normalize(text).split("[^a-z0-9-]+"))
                .filter(word -> !word.isBlank())
                .toList();
    }

    static String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }
}
