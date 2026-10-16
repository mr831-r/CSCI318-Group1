package com.larp.database;

import java.time.LocalDate;

/**
 * Common fields and update behavior required by generic database operations.
 */
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