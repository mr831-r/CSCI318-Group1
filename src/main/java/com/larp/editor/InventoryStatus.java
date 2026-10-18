package com.larp.editor;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

public enum InventoryStatus {
    AVAILABLE,
    SHIPPING,
    ARRIVED;

    @JsonCreator
    public static InventoryStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        return InventoryStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
