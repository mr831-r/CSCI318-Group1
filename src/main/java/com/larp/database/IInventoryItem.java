package com.larp.database;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.time.LocalDate;

@JsonDeserialize(as = InventoryItem.class)
public interface IInventoryItem extends IDatabaseItem {
    String getName();

    void setName(String name);

    String getDescription();

    void setDescription(String description);

    Integer getQuantity();

    void setQuantity(Integer quantity);

    String getStatus();

    void setStatus(String status);

    LocalDate getDate();

    void setDate(LocalDate date);

    String getPayslipReference();

    void setPayslipReference(String payslipReference);

    void setItemCode(String itemCode);
}