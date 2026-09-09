package com.larp.database;

public interface DatabaseItem {

    default String getFilterName() {
        return getClass().getSimpleName();
    }

    default String getItemCode() {
        return null;
    }
}
