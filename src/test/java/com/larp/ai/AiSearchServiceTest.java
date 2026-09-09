package com.larp.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiSearchServiceTest {

    @Test
    void defaultModelAndProviderUseGemini() {
        assertEquals("gemini-2.0-flash", AiSearchService.defaultModel());
        assertEquals(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent",
                AiSearchService.defaultProviderUrl()
        );
    }
}
