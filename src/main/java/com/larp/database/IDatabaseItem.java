package com.larp.database;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.larp.ai.TermAlias;
import com.larp.editor.InventoryItem;
import com.larp.logviewer.LogEntry;
import com.larp.shipping.ShippingRequest;

import java.time.LocalDate;

/**
 * Common fields and update behavior required by generic database operations.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes({
        @JsonSubTypes.Type(InventoryItem.class),
        @JsonSubTypes.Type(LogEntry.class),
        @JsonSubTypes.Type(ShippingRequest.class),
        @JsonSubTypes.Type(TermAlias.class)
})
public interface IDatabaseItem {

    Long getId();

    /**
     * A filter name used by the DatabaseHostService when listing or querying
     * items. Default is the simple class name of the implementation.
     */
    default String getFilterName() {
        return getClass().getSimpleName();
    }

    /**
     * Optional short code identifying the item (e.g. item code or SKU).
     */
    default String getItemCode() {
        return null;
    }

    default LocalDate getDate() {
        return null;
    }

    void updateFrom(IDatabaseItem incoming);
}