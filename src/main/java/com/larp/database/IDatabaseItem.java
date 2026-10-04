package com.larp.database;

/**
 * Primary database item interface used across microservices.
 * this interface defines the
 * minimal shape required by the refined user stories.
 */
public interface IDatabaseItem {

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
}