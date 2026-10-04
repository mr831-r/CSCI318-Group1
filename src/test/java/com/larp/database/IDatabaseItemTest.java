package com.larp.database;

import com.larp.logviewer.LogEntry;
import com.larp.shipping.ShippingRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class IDatabaseItemTest {

    @Test
    void entitiesImplementIDatabaseItem() {
        assertTrue(IDatabaseItem.class.isAssignableFrom(InventoryItem.class), "InventoryItem should implement IDatabaseItem");
        assertTrue(IDatabaseItem.class.isAssignableFrom(LogEntry.class), "LogEntry should implement IDatabaseItem");
        assertTrue(IDatabaseItem.class.isAssignableFrom(ShippingRequest.class), "ShippingRequest should implement IDatabaseItem");
    }
}
