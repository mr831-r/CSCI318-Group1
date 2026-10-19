package com.larp.ai;

import dev.langchain4j.service.Result;
import dev.langchain4j.service.tool.ToolExecution;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The agentic side of the AI Assistant: the LLM chooses which tools to call, chains them, remembers the
 * conversation and proposes changes that a person approves. Shares the /ai path with {@link AiSearchService}.
 *
 * <pre>
 * POST   /ai/chat                    {"sessionId": "optional", "message": "..."}  -> AgentChatResponse
 * GET    /ai/status                  provider, model and whether an API key is configured
 * GET    /ai/actions?sessionId=      proposals made by the agent (newest first)
 * POST   /ai/actions/{id}/approve    run a proposal through EditorService / ShippingService
 * POST   /ai/actions/{id}/reject     discard a proposal
 * DELETE /ai/sessions/{sessionId}    forget a conversation
 * </pre>
 */
@Service
@RestController
@RequestMapping("/ai")
public class AgentService {

    static final String NOT_CONFIGURED = "The AI agent has no LLM API key configured. Set AI_API_KEY (and optionally "
            + "AI_PROVIDER=gemini|openai|anthropic) and restart the application. Keyword search at /ai still works.";

    private final AgentRuntime runtime;
    private final AgentActionService actions;
    private final AgentEventPublisher events;

    public AgentService(AgentRuntime runtime, AgentActionService actions, AgentEventPublisher events) {
        this.runtime = runtime;
        this.actions = actions;
        this.events = events;
    }

    public record AgentChatRequest(String sessionId, String message) {
    }

    /** One tool call the agent made while answering, so the user can see how it reached its answer. */
    public record AgentStep(String tool, String arguments, String result) {
    }

    public record AgentChatResponse(
            String sessionId,
            String reply,
            boolean configured,
            List<AgentStep> steps,
            List<PendingAction> pendingActions) {
    }

    public AgentChatResponse chat(String sessionId, String message) {
        String session = sessionId == null || sessionId.isBlank()
                ? UUID.randomUUID().toString().substring(0, 8)
                : sessionId.trim();
        if (message == null || message.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A message is required");
        }

        Optional<Result<String>> result;
        try {
            result = runtime.chat(session, message.trim());
        } catch (RuntimeException ex) {
            events.publish("CHAT_FAILED", session, ex.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "The LLM provider call failed: " + ex.getMessage(), ex);
        }
        if (result.isEmpty()) {
            return new AgentChatResponse(session, NOT_CONFIGURED, false, List.of(), actions.pending(session));
        }

        List<AgentStep> steps = result.get().toolExecutions() == null ? List.of()
                : result.get().toolExecutions().stream().map(AgentService::toStep).toList();
        String reply = result.get().content();
        events.publish("CHAT", session, "Q: " + message.trim() + " | tools: "
                + steps.stream().map(AgentStep::tool).toList() + " | A: " + reply);
        return new AgentChatResponse(session, reply, true, steps, actions.pending(session));
    }

    @PostMapping("/chat")
    public AgentChatResponse chatEndpoint(@RequestBody AgentChatRequest request) {
        return chat(request.sessionId(), request.message());
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("configured", runtime.isConfigured());
        status.put("provider", runtime.description());
        status.put("pendingActions", actions.pending(null).size());
        if (!runtime.isConfigured()) {
            status.put("hint", NOT_CONFIGURED);
        }
        return status;
    }

    @GetMapping("/actions")
    public List<PendingAction> listActions(@RequestParam(value = "sessionId", required = false) String sessionId) {
        return actions.list(sessionId);
    }

    @PostMapping("/actions/{id}/approve")
    public PendingAction approve(@PathVariable String id) {
        return resolve(() -> actions.approve(id));
    }

    @PostMapping("/actions/{id}/reject")
    public PendingAction reject(@PathVariable String id) {
        return resolve(() -> actions.reject(id));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public void forget(@PathVariable String sessionId) {
        runtime.forget(sessionId);
    }

    /** Return the reason as JSON ({"message": ...}) so the console can show it. Only applies to this controller. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleError(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of("message", ex.getReason() == null ? ex.getStatusCode().toString() : ex.getReason()));
    }

    private static PendingAction resolve(java.util.function.Supplier<PendingAction> body) {
        try {
            return body.get();
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
    }

    private static AgentStep toStep(ToolExecution execution) {
        return new AgentStep(
                execution.request().name(),
                execution.request().arguments(),
                AgentEventPublisher.truncate(execution.result(), 1000));
    }
}
