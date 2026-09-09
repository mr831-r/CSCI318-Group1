package com.larp.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    List<InventoryItem> findByItemCode(String itemCode);

    @Query("SELECT i FROM InventoryItem i WHERE i.itemCode = :itemCode AND i.date BETWEEN :start AND :end ORDER BY i.date DESC")
    List<InventoryItem> findByItemCodeBetweenDates(@Param("itemCode") String itemCode,
                                                 @Param("start") LocalDate start,
                                                 @Param("end") LocalDate end);

    List<InventoryItem> findByDescriptionContainingIgnoreCase(String description);
}
