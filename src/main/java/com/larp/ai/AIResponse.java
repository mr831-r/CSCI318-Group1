package com.larp.ai;

import com.larp.editor.InventoryItem;

import java.time.LocalDate;
import java.util.List;

public class AIResponse {
    private String query;
    private LocalDate startDate;
    private LocalDate endDate;
    private int count;
    private String summary;
    private List<InventoryItem> results;
    private boolean llmConfigured;
    private String model;
    private String providerUrl;

    public AIResponse() {}

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<InventoryItem> getResults() {
        return results;
    }

    public void setResults(List<InventoryItem> results) {
        this.results = results;
    }

    public boolean isLlmConfigured() {
        return llmConfigured;
    }

    public void setLlmConfigured(boolean llmConfigured) {
        this.llmConfigured = llmConfigured;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getProviderUrl() {
        return providerUrl;
    }

    public void setProviderUrl(String providerUrl) {
        this.providerUrl = providerUrl;
    }
}