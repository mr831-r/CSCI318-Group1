package com.larp.shipping;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.larp.database.IDatabaseItem;

import java.time.LocalDate;

@JsonDeserialize(as = ShippingRequest.class)
public interface IShippingRequest extends IDatabaseItem {
    void setItemCode(String itemCode);

    String getDestination();

    void setDestination(String destination);

    Integer getQuantity();

    void setQuantity(Integer quantity);

    String getStatus();

    void setStatus(String status);

    LocalDate getRequestedDate();

    void setRequestedDate(LocalDate requestedDate);
}
