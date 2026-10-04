package com.larp.logviewer;

import com.larp.database.DatabaseService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LogServiceTest {

    @Test
    void addLogEntryBuildsAndSavesLogEntryFromPayload() {
        DatabaseService databaseService = mock(DatabaseService.class);
        when(databaseService.saveLogEntry(any(LogEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        LogService service = new LogService(databaseService);

        LogEntry result = service.addLogEntry(Map.of("message", "Import complete", "level", "INFO"));

        assertEquals("Import complete", result.getMessage());
        assertEquals("INFO", result.getLevel());
    }
}