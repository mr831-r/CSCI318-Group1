package com.larp.logviewer;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LogServiceTest {

    @Test
    void addLogEntryBuildsAndSavesLogEntryFromPayload() {
        LogEntryRepository repository = mock(LogEntryRepository.class);
        when(repository.save(any(LogEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        LogService service = new LogService(repository);

        LogEntry result = service.addLogEntry(Map.of("message", "Import complete", "level", "INFO"));

        assertEquals("Import complete", result.getMessage());
        assertEquals("INFO", result.getLevel());
    }
}