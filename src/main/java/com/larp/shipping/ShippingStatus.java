package com.larp.shipping;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;

public enum ShippingStatus {
<<<<<<< HEAD
    /** Requested by a camp, waiting for stock to be dispatched. */
    PENDING,
    /** Stock was sent to the destination by a courier. */
    DISPATCHED,
    /** Incoming stock was received. */
=======
    PENDING,
>>>>>>> 3aee914b11d4fe54347f4e181b6413330c75b1b7
    ARRIVED;

    @JsonCreator
    public static ShippingStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        return ShippingStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
