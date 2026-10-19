package com.larp.ai;

import com.larp.database.DatabaseService;
import com.larp.editor.InventoryItem;
import com.larp.logviewer.LogEntry;
import com.larp.logviewer.LogService;
import com.larp.shipping.ShippingRequest;
import com.larp.shipping.ShippingService;
import com.larp.shipping.ShippingStatus;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * The tools the LLM can choose to call. Each tool wraps a public method of an existing LARP service, so the
 * agent works through the same rules as the REST API. Read tools run immediately; tools that would change
 * stock only create a {@link PendingAction} that a person must approve.
 *
 * Tools return plain text (one record per line) because that is compact and easy for a model to read.
 * Errors are returned as text starting with "ERROR:" so the model can recover instead of the request failing.
 */
@Component
public class AgentTools {

    static final int MAX_ROWS = 25;

    private final DatabaseService databaseService;
    private final LogService logService;
    private final ShippingService shippingService;
    private final TerminologyService terminology;
    private final AiSearchService search;
    private final AgentActionService actions;
    private final AgentEventPublisher events;

    public AgentTools(
            DatabaseService databaseService,
            LogService logService,
            ShippingService shippingService,
            TerminologyService terminology,
            AiSearchService search,
            AgentActionService actions,
            AgentEventPublisher events) {
        this.databaseService = databaseService;
        this.logService = logService;
        this.shippingService = shippingService;
        this.terminology = terminology;
        this.search = search;
        this.actions = actions;
        this.events = events;
    }

    // ---------------------------------------------------------------- read tools

    @Tool("Search the inventory by keywords (name, description, item code or status). Learned terminology is "
            + "applied automatically. Returns at most 25 items, best matches first. Use an empty query to list everything.")
    public String searchInventory(
            @P("Keywords to search for, e.g. 'healing potion' or 'blade'. May be empty.") String query) {
        return safely(() -> {
            Set<String> words = terminology.expand(query);
            List<InventoryItem> matches = search.search(query, null, null, MAX_ROWS);
            if (matches.isEmpty()) {
                return "No inventory items matched " + words + ". If the user used an unfamiliar word, ask what it "
                        + "means or search with a related word, then call learnTerm so future searches understand it.";
            }
            String header = words.isEmpty() ? "All inventory" : "Matches for " + words;
            return header + " (" + matches.size() + "):\n" + formatItems(matches);
        });
    }

    @Tool("Get the full current record for one inventory item by its item code, e.g. ITEM-1001.")
    public String getItem(@P("The item code") String itemCode) {
        return safely(() -> findItem(itemCode)
                .map(AgentTools::formatItem)
                .orElse("ERROR: no inventory item has code " + itemCode));
    }

    @Tool("List inventory items whose quantity is at or below a threshold, lowest first.")
    public String findLowStock(@P("Quantity threshold, e.g. 5") int threshold) {
        return safely(() -> {
            List<InventoryItem> low = inventory().stream()
                    .filter(item -> item.getQuantity() != null && item.getQuantity() <= threshold)
                    .sorted(Comparator.comparing(InventoryItem::getQuantity))
                    .toList();
            return low.isEmpty()
                    ? "No items have quantity <= " + threshold
                    : "Items with quantity <= " + threshold + ":\n" + formatItems(low);
        });
    }

    @Tool("Get an item's current record plus every audit log entry that mentions it (its edit history).")
    public String getItemHistory(@P("The item code") String itemCode) {
        return safely(() -> {
            String code = itemCode == null ? "" : itemCode.trim();
            String current = findItem(code).map(AgentTools::formatItem).orElse("No current record for " + code);
            List<LogEntry> entries = logService.findLogs(null, null).stream()
                    .filter(entry -> entry.getMessage() != null && entry.getMessage().contains(code))
                    .limit(MAX_ROWS)
                    .toList();
            return "Current: " + current + "\nHistory (newest first):\n" + formatLogs(entries);
        });
    }

    @Tool("List shipping records: requests from camps (PENDING, then DISPATCHED) and stock arrivals (ARRIVED). "
            + "Optionally filter by item code and/or status. Requests are listed oldest (most urgent) first.")
    public String listShipments(
            @P(value = "Item code to filter by, or empty for all", required = false) String itemCode,
            @P(value = "PENDING, DISPATCHED, ARRIVED, or empty for all", required = false) String status) {
        return safely(() -> {
            List<ShippingRequest> records = new ArrayList<>(shippingService.getRequestQueue(null));
            shippingService.getShippingRequests().stream()
                    .filter(record -> record.getStatus() == ShippingStatus.ARRIVED)
                    .forEach(records::add);
            List<ShippingRequest> shipments = records.stream()
                    .filter(s -> blank(itemCode) || itemCode.trim().equalsIgnoreCase(s.getItemCode()))
                    .filter(s -> blank(status) || (s.getStatus() != null
                            && s.getStatus().name().equalsIgnoreCase(status.trim())))
                    .limit(MAX_ROWS)
                    .toList();
            if (shipments.isEmpty()) {
                return "No shipping records match.";
            }
            return shipments.stream()
                    .map(s -> "request " + s.getId() + " | " + s.getItemCode() + " | qty=" + s.getQuantity()
                            + " | destination=" + s.getDestination() + " | status=" + s.getStatus()
                            + " | date=" + s.getRequestedDate()
                            + (s.getCourier() == null ? "" : " | courier=" + s.getCourier()))
                    .collect(Collectors.joining("\n"));
        });
    }

    @Tool("Read audit log entries between two dates (inclusive, ISO format yyyy-MM-dd). Either date may be empty.")
    public String readLogs(
            @P(value = "Start date yyyy-MM-dd, or empty", required = false) String startDate,
            @P(value = "End date yyyy-MM-dd, or empty", required = false) String endDate) {
        return safely(() -> {
            List<LogEntry> entries = logService.findLogs(parseDate(startDate), parseDate(endDate)).stream()
                    .limit(MAX_ROWS)
                    .toList();
            return entries.isEmpty() ? "No log entries in that range." : formatLogs(entries);
        });
    }

    @Tool("List all terminology learned so far (term -> meaning).")
    public String listLearnedTerms() {
        return safely(() -> {
            List<TermAlias> aliases = terminology.all();
            if (aliases.isEmpty()) {
                return "No terms learned yet.";
            }
            return aliases.stream()
                    .map(alias -> alias.getTerm() + " -> " + alias.getMeaning())
                    .collect(Collectors.joining("\n"));
        });
    }

    // ---------------------------------------------------------------- learning tool

    @Tool("Remember what an unfamiliar word means so future searches understand it, e.g. term 'blade' means "
            + "'sword dagger'. Only call this once the meaning is clear from the user or from the inventory.")
    public String learnTerm(
            @ToolMemoryId String sessionId,
            @P("The unfamiliar word or phrase") String term,
            @P("Words it should expand to when searching, space separated, e.g. 'sword dagger'") String meaning) {
        return safely(() -> {
            TermAlias alias = terminology.learn(term, meaning, sessionId);
            events.publish("TERM_LEARNED", sessionId, alias.getTerm() + " -> " + alias.getMeaning());
            return "Learned: '" + alias.getTerm() + "' now also searches for '" + alias.getMeaning() + "'.";
        });
    }

    // ---------------------------------------------------------------- proposal tools (need human approval)

    @Tool("Propose setting an inventory item's quantity to a new absolute value. This does NOT change anything "
            + "until a person approves it; tell the user the action id.")
    public String proposeQuantityChange(
            @ToolMemoryId String sessionId,
            @P("The item code") String itemCode,
            @P("The new total quantity (not a difference)") int newQuantity,
            @P("Why this change is needed") String reason) {
        return safely(() -> proposed(actions.proposeQuantity(sessionId, itemCode, newQuantity, reason)));
    }

    @Tool("Propose changing an inventory item's status to AVAILABLE, SHIPPING or ARRIVED. This does NOT change "
            + "anything until a person approves it; tell the user the action id.")
    public String proposeStatusChange(
            @ToolMemoryId String sessionId,
            @P("The item code") String itemCode,
            @P("AVAILABLE, SHIPPING or ARRIVED") String status,
            @P("Why this change is needed") String reason) {
        return safely(() -> proposed(actions.proposeStatus(sessionId, itemCode, status, reason)));
    }

    @Tool("Propose recording that stock has arrived (adds the quantity to the item and marks it ARRIVED). This "
            + "does NOT change anything until a person approves it; tell the user the action id.")
    public String proposeStockArrival(
            @ToolMemoryId String sessionId,
            @P("The item code") String itemCode,
            @P("How many units arrived") int quantity,
            @P("Where the stock arrived, e.g. 'North Camp'") String destination,
            @P("Why this change is needed") String reason) {
        return safely(() -> proposed(actions.proposeArrival(sessionId, itemCode, quantity, destination, reason)));
    }

    @Tool("Propose dispatching a PENDING shipping request with a courier, which takes its quantity out of stock. "
            + "Check stock first. This does NOT change anything until a person approves it; tell the user the action id.")
    public String proposeDispatch(
            @ToolMemoryId String sessionId,
            @P("The shipping request id (the number after 'request' in listShipments)") long requestId,
            @P("Courier to assign, e.g. 'Fast Horse Couriers'") String courier,
            @P("Why this request should go now") String reason) {
        return safely(() -> proposed(actions.proposeDispatch(sessionId, requestId, courier, reason)));
    }

    // ---------------------------------------------------------------- helpers

    private List<InventoryItem> inventory() {
        return databaseService.findItems("logistics", null, null, null).stream()
                .map(InventoryItem.class::cast)
                .toList();
    }

    private Optional<InventoryItem> findItem(String itemCode) {
        if (blank(itemCode)) {
            return Optional.empty();
        }
        return databaseService.findLatestItem("logistics", itemCode.trim()).map(InventoryItem.class::cast);
    }

    private static String proposed(PendingAction action) {
        return "Proposed action " + action.id() + ": " + action.describe()
                + ". It is waiting for a person to approve it; nothing has changed yet.";
    }

    static String formatItems(List<InventoryItem> items) {
        return items.stream().limit(MAX_ROWS).map(AgentTools::formatItem).collect(Collectors.joining("\n"));
    }

    static String formatItem(InventoryItem item) {
        return item.getItemCode() + " | " + item.getName() + " | " + item.getDescription()
                + " | qty=" + item.getQuantity() + " | status=" + item.getStatus() + " | date=" + item.getDate()
                + " | payslip=" + item.getPayslipReference();
    }

    static String formatLogs(List<LogEntry> entries) {
        if (entries.isEmpty()) {
            return "(none)";
        }
        return entries.stream()
                .map(entry -> entry.getTimestamp() + " " + entry.getLevel() + " " + entry.getMessage())
                .collect(Collectors.joining("\n"));
    }

    private static LocalDate parseDate(String value) {
        if (blank(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Dates must be yyyy-MM-dd, got '" + value + "'");
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String safely(Supplier<String> body) {
        try {
            return body.get();
        } catch (RuntimeException ex) {
            return "ERROR: " + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
        }
    }
}
