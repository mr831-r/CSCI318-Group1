package com.larp.ai;

/**
 * Search request for PUT/POST /ai. The LLM provider and key are server configuration (AI_PROVIDER, AI_API_KEY),
 * never part of a request.
 */
public class AIRequest {
    private String query;
    private String startDate;
    private String endDate;

    public AIRequest() {}

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }
}
