package com.larp.shipping;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

public enum ShippingStatus {
    PENDING,
    ARRIVED;

    @JsonCreator
    public static ShippingStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        return ShippingStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
