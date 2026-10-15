package com.larp.database;

import java.time.LocalDate;

/**
 * Primary database item interface used across microservices.
 * this interface defines the
 * minimal shape required by the refined user stories.
 */

// TODO: Add anything that generic IDatabaseItem functions require here, and implement on all classes extending it.
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
        return LocalDate.now();
    }
}