package com.larp.ai;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.tool.ToolArgumentsErrorHandler;
import dev.langchain4j.service.tool.ToolExecutionErrorHandler;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Builds the LangChain4j agent (model + tools + per-session memory + system prompt) and runs it.
 */
@Component
public class AgentRuntime {

    static final String SYSTEM_PROMPT = """
            You are the LARP logistics agent for a company that stores and ships LARP equipment.
            Today is %s.

            How to work:
            - Use your tools to look things up. Never invent item codes, quantities, dates or log entries.
            - Plan, then call as many tools as you need (e.g. search, then get the item, then read its history).
            - Inventory statuses are AVAILABLE, SHIPPING and ARRIVED.
            - Shipping records: PENDING = a camp requested stock, DISPATCHED = sent with a courier,
              ARRIVED = incoming stock was received.
            - If a search finds nothing because the user used an unfamiliar word, search with related words.
              Once you know what the word means (from the user or the results), call learnTerm.
            - You cannot change stock directly. To change anything, call a propose... tool, then tell the user the
              action id and that a person must approve it. Never claim a change has been made.
            - When matching stock to PENDING requests: oldest requested date first, and only dispatch what is
              actually in stock. Use proposeDispatch with a courier for requests that can be filled.
            - Answer concisely. Quote item codes. Use a short list when giving several items.
            """;

    private final AgentSettings settings;
    private final InMemoryChatMemoryStore memoryStore = new InMemoryChatMemoryStore();
    private final LarpAgent agent;
    private final String description;

    @Autowired
    public AgentRuntime(AgentSettings settings, AgentTools tools) {
        this(settings, tools, settings.createChatModel().orElse(null),
                settings.provider() + " / " + settings.model());
    }

    /** Also used by tests to plug in a scripted model. */
    AgentRuntime(AgentSettings settings, AgentTools tools, ChatModel chatModel, String description) {
        this.settings = settings;
        this.description = description;
        this.agent = chatModel == null ? null : AiServices.builder(LarpAgent.class)
                .chatModel(chatModel)
                .tools(new Object[]{tools})
                .chatMemoryProvider(sessionId -> MessageWindowChatMemory.builder()
                        .id(sessionId)
                        .maxMessages(settings.memoryMessages())
                        .chatMemoryStore(memoryStore)
                        .build())
                .systemMessageProvider(sessionId -> SYSTEM_PROMPT.formatted(LocalDate.now()))
                .maxSequentialToolsInvocations(settings.maxToolCalls())
                // Bad arguments from the model go back to it so it can retry. Our tools already catch their own
                // errors and return "ERROR: ..." text, so any other exception is a real bug and fails the request
                // instead of leaking internal details to the LLM provider.
                .toolArgumentsErrorHandler(ToolArgumentsErrorHandler.sendExceptionMessageToLlm())
                .toolExecutionErrorHandler(ToolExecutionErrorHandler.failInvocationUnlessVisibleToLlm())
                .build();
    }

    public boolean isConfigured() {
        return agent != null;
    }

    /** "provider / model", for status output. */
    public String description() {
        return description;
    }

    public Optional<Result<String>> chat(String sessionId, String message) {
        if (agent == null) {
            return Optional.empty();
        }
        return Optional.of(agent.chat(sessionId, message));
    }

    public void forget(String sessionId) {
        memoryStore.deleteMessages(sessionId);
    }

    AgentSettings settings() {
        return settings;
    }
}
