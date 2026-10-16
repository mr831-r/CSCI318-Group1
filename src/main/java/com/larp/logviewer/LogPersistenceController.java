package com.larp.logviewer;

import com.larp.database.DatabaseService;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LogPersistenceController {

    private final DatabaseService databaseService;

    public LogPersistenceController(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    @PutMapping("/database/logs")
    public LogEntry persistLog(@RequestBody LogEntry entry) {
        return databaseService.createItem(entry);
    }
}
