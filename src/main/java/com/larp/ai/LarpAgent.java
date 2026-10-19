package com.larp.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.UserMessage;

/**
 * The agent as LangChain4j sees it. LangChain4j generates the implementation: on each call it sends the
 * conversation memory, the system prompt and the {@link AgentTools} descriptions to the model, runs whatever
 * tools the model asks for, feeds the results back, and repeats until the model gives a final answer.
 */
public interface LarpAgent {

    /**
     * @param sessionId conversation id; each id has its own memory
     * @param message   what the user (or the autonomous trigger) said
     * @return the final answer plus the list of tool calls the agent made on the way
     */
    Result<String> chat(@MemoryId String sessionId, @UserMessage String message);
}
