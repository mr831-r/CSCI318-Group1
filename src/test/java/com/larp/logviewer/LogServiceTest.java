package com.larp.logviewer;

import com.larp.database.DatabaseService;
import com.larp.events.AiEvent;
import com.larp.events.ItemEvent;
import com.larp.events.StockEvent;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.PUT;

class LogServiceTest {

    @Test
    void addLogEntryBuildsAndSavesLogEntryFromPayload() {
        DatabaseService databaseService = mock(DatabaseService.class);
        when(databaseService.createItem(any(LogEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        LogService service = new LogService(databaseService, RestClient.builder(), "http://localhost:48484");

        LogEntry result = service.addLogEntry(Map.of("message", "Import complete", "level", "INFO"));

        assertEquals("Import complete", result.getMessage());
        assertEquals("INFO", result.getLevel());
    }

    @Test
    void findLogsFiltersByInclusiveDateRangeAndSortsNewestFirst() {
        DatabaseService databaseService = mock(DatabaseService.class);
        LogEntry earlier = logAt(LocalDateTime.of(2026, 10, 13, 23, 59));
        LogEntry latest = logAt(LocalDateTime.of(2026, 10, 14, 23, 59));
        LogEntry outsideRange = logAt(LocalDateTime.of(2026, 10, 15, 0, 0));
        when(databaseService.findAllItems(LogEntry.class))
                .thenReturn(List.of(earlier, latest, outsideRange));

        List<LogEntry> result = new LogService(databaseService, RestClient.builder(), "http://localhost:48484")
                .findLogs(LocalDate.of(2026, 10, 14), LocalDate.of(2026, 10, 14));

        assertEquals(List.of(latest), result);
    }

    @Test
    void itemEventConsumerWritesLogThroughDatabaseRestEndpoint() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://localhost:48484/database/logs"))
                .andExpect(method(PUT))
                .andRespond(withSuccess("""
                        {"id":1,"message":"ITEM_ADDED for item ITEM-1","level":"INFO","timestamp":"2026-10-14T10:00:00"}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://localhost:48484/database/logs"))
                .andExpect(method(PUT))
                .andRespond(withSuccess("""
                        {"id":2,"message":"AI query: sword","level":"INFO","timestamp":"2026-10-14T10:00:00"}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://localhost:48484/database/logs"))
                .andExpect(method(PUT))
                .andRespond(withSuccess("""
                        {"id":3,"message":"STOCK_ARRIVED for item ITEM-1, quantity delta 4","level":"INFO","timestamp":"2026-10-14T10:00:00"}
                        """, MediaType.APPLICATION_JSON));

        LogService service = new LogService(mock(DatabaseService.class), builder, "http://localhost:48484");
        service.logItemEvents().accept(new ItemEvent(
                "ITEM_ADDED", "ITEM-1", Instant.parse("2026-10-14T10:00:00Z")));
        service.logAiEvents().accept(new AiEvent("sword", Instant.parse("2026-10-14T10:00:00Z")));
        service.logStockEvents().accept(new StockEvent(
                "STOCK_ARRIVED", "ITEM-1", 4, Instant.parse("2026-10-14T10:00:00Z")));

        server.verify();
    }

    private LogEntry logAt(LocalDateTime timestamp) {
        LogEntry entry = new LogEntry();
        entry.setTimestamp(timestamp);
        return entry;
    }
}