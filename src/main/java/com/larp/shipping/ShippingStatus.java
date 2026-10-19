package com.larp.shipping;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

public enum ShippingStatus {
    /** Requested by a camp, waiting for stock to be dispatched. */
    PENDING,
    /** Stock was sent to the destination by a courier. */
    DISPATCHED,
    /** Incoming stock was received. */
    ARRIVED;

    @JsonCreator
    public static ShippingStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        return ShippingStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
