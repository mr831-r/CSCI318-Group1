package com.larp.logviewer;

import com.larp.database.IDatabaseItem;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "log_entries")
public class LogEntry implements ILogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private String level;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    public LogEntry() {
    }

    @Override
    public String getFilterName() {
        return "log";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public java.time.LocalDate getDate() {
        return timestamp == null ? null : timestamp.toLocalDate();
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public void updateFrom(IDatabaseItem incoming) {
        if (!(incoming instanceof ILogEntry entry)) {
            throw new IllegalArgumentException("Log entries can only be updated from log entries");
        }
        if (entry.getMessage() != null) {
            setMessage(entry.getMessage());
        }
        if (entry.getLevel() != null) {
            setLevel(entry.getLevel());
        }
        if (entry.getTimestamp() != null) {
            setTimestamp(entry.getTimestamp());
        }
    }
}
