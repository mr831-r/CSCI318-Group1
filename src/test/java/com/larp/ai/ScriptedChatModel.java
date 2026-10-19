package com.larp.ai;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Function;

/**
 * A fake LLM for tests: plays back a fixed script of replies (tool calls or text), so agent behaviour can be
 * tested without an API key or network. Every request it receives is recorded for assertions.
 */
class ScriptedChatModel implements ChatModel {

    private final Deque<Function<ChatRequest, AiMessage>> script = new ArrayDeque<>();
    final List<ChatRequest> requests = new ArrayList<>();

    ScriptedChatModel callsTool(String name, String argumentsJson) {
        script.add(request -> AiMessage.from(ToolExecutionRequest.builder()
                .id("call-" + script.size())
                .name(name)
                .arguments(argumentsJson)
                .build()));
        return this;
    }

    /** Final answer that echoes the most recent tool result, like a model summarising what it found. */
    ScriptedChatModel answersWithLastToolResult(String prefix) {
        script.add(request -> AiMessage.from(prefix + lastToolResult(request)));
        return this;
    }

    ScriptedChatModel answers(String text) {
        script.add(request -> AiMessage.from(text));
        return this;
    }

    @Override
    public ChatResponse doChat(ChatRequest request) {
        requests.add(request);
        if (script.isEmpty()) {
            throw new IllegalStateException("ScriptedChatModel ran out of scripted replies");
        }
        return ChatResponse.builder().aiMessage(script.poll().apply(request)).build();
    }

    static String lastToolResult(ChatRequest request) {
        List<ChatMessage> messages = request.messages();
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i) instanceof ToolExecutionResultMessage result) {
                return result.text();
            }
        }
        return "";
    }
}
