package com.larp.logviewer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface LogEntryRepository extends JpaRepository<LogEntry, Long> {

    @Query("SELECT l FROM LogEntry l WHERE l.timestamp BETWEEN :start AND :end ORDER BY l.timestamp DESC")
    List<LogEntry> findByTimestampBetween(@Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);
}
