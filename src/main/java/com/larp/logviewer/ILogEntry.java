package com.larp.logviewer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.larp.database.IDatabaseItem;

import java.time.LocalDateTime;

@JsonDeserialize(as = LogEntry.class)
public interface ILogEntry extends IDatabaseItem {
    String getMessage();

    void setMessage(String message);

    String getLevel();

    void setLevel(String level);

    LocalDateTime getTimestamp();

    void setTimestamp(LocalDateTime timestamp);
}