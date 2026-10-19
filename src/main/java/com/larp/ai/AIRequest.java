package com.larp.ai;

<<<<<<< HEAD
/**
 * Search request for PUT/POST /ai. The LLM provider and key are server configuration (AI_PROVIDER, AI_API_KEY),
 * never part of a request.
 */
=======
>>>>>>> 3aee914b11d4fe54347f4e181b6413330c75b1b7
public class AIRequest {
    private String query;
    private String startDate;
    private String endDate;
<<<<<<< HEAD
=======
    private String apiKey;
    private String model;
    private String providerUrl;
>>>>>>> 3aee914b11d4fe54347f4e181b6413330c75b1b7

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
<<<<<<< HEAD
}
=======

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
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
>>>>>>> 3aee914b11d4fe54347f4e181b6413330c75b1b7
