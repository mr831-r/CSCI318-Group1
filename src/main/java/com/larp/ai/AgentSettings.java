package com.larp.ai;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

/**
 * AI Assistant configuration (larp.ai.* in application.yml, normally supplied as environment variables):
 *
 * <pre>
 * AI_PROVIDER    gemini (default) | openai | anthropic
 * AI_API_KEY     API key; falls back to GEMINI_API_KEY / OPENAI_API_KEY / ANTHROPIC_API_KEY for the provider
 * AI_MODEL       model name; a sensible default is chosen per provider
 * AI_BASE_URL    optional, for OpenAI-compatible servers (e.g. a local Ollama at http://localhost:11434/v1)
 * </pre>
 *
 * Without an API key the application still starts; the agent endpoints report that the agent is not configured.
 */
@Component
public class AgentSettings {

    private final String provider;
    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final int maxToolCalls;
    private final int memoryMessages;

    public AgentSettings(
            @Value("${larp.ai.provider:gemini}") String provider,
            @Value("${larp.ai.api-key:}") String apiKey,
            @Value("${larp.ai.model:}") String model,
            @Value("${larp.ai.base-url:}") String baseUrl,
            @Value("${larp.ai.max-tool-calls:10}") int maxToolCalls,
            @Value("${larp.ai.memory-messages:20}") int memoryMessages) {
        this.provider = provider == null || provider.isBlank() ? "gemini" : provider.trim().toLowerCase(Locale.ROOT);
        this.apiKey = firstNonBlank(apiKey, System.getenv(providerKeyVariable(this.provider)));
        this.model = firstNonBlank(model, defaultModel(this.provider));
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
        this.maxToolCalls = maxToolCalls;
        this.memoryMessages = memoryMessages;
    }

    /** Builds the chat model for the configured provider, or empty when no API key is set. */
    public Optional<ChatModel> createChatModel() {
        if (!isConfigured()) {
            return Optional.empty();
        }
        Duration timeout = Duration.ofSeconds(60);
        ChatModel chatModel = switch (provider) {
            case "openai" -> {
                OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                        .apiKey(apiKey)
                        .modelName(model)
                        .temperature(0.2)
                        .timeout(timeout);
                if (!baseUrl.isBlank()) {
                    builder.baseUrl(baseUrl);
                }
                yield builder.build();
            }
            case "anthropic" -> AnthropicChatModel.builder()
                    .apiKey(apiKey)
                    .modelName(model)
                    .maxTokens(1024)
                    .timeout(timeout)
                    .build();
            case "gemini" -> GoogleAiGeminiChatModel.builder()
                    .apiKey(apiKey)
                    .modelName(model)
                    .temperature(0.2)
                    .timeout(timeout)
                    .build();
            default -> throw new IllegalStateException(
                    "Unknown AI_PROVIDER '" + provider + "'. Use gemini, openai or anthropic.");
        };
        return Optional.of(chatModel);
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String provider() {
        return provider;
    }

    public String model() {
        return model;
    }

    public int maxToolCalls() {
        return maxToolCalls;
    }

    public int memoryMessages() {
        return memoryMessages;
    }

    static String defaultModel(String provider) {
        return switch (provider) {
            case "openai" -> "gpt-4.1-mini";
            case "anthropic" -> "claude-haiku-5-5";
            default -> "gemini-2.5-flash";
        };
    }

    static String providerKeyVariable(String provider) {
        return switch (provider) {
            case "openai" -> "OPENAI_API_KEY";
            case "anthropic" -> "ANTHROPIC_API_KEY";
            default -> "GEMINI_API_KEY";
        };
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        return second == null ? "" : second.trim();
    }
}
