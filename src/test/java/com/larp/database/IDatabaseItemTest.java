package com.larp.database;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.larp.logviewer.LogEntry;
import com.larp.logviewer.ILogEntry;
import com.larp.shipping.ShippingRequest;
import com.larp.shipping.IShippingRequest;
import com.larp.editor.InventoryItem;
import com.larp.editor.IInventoryItem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class IDatabaseItemTest {

    @Test
    void entitiesImplementIDatabaseItem() {
        assertTrue(IDatabaseItem.class.isAssignableFrom(InventoryItem.class), "InventoryItem should implement IDatabaseItem");
        assertTrue(IDatabaseItem.class.isAssignableFrom(LogEntry.class), "LogEntry should implement IDatabaseItem");
        assertTrue(IDatabaseItem.class.isAssignableFrom(ShippingRequest.class), "ShippingRequest should implement IDatabaseItem");
        assertTrue(IInventoryItem.class.isAssignableFrom(InventoryItem.class), "InventoryItem should implement IInventoryItem");
        assertTrue(ILogEntry.class.isAssignableFrom(LogEntry.class), "LogEntry should implement ILogEntry");
        assertTrue(IShippingRequest.class.isAssignableFrom(ShippingRequest.class), "ShippingRequest should implement IShippingRequest");
    }

    @Test
    void domainInterfacesDeserializeIntoTheirEntities() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        assertTrue(mapper.readValue("{\"itemCode\":\"ITEM-1\"}", IInventoryItem.class) instanceof InventoryItem);
        assertTrue(mapper.readValue("{\"message\":\"Saved\"}", ILogEntry.class) instanceof LogEntry);
        assertTrue(mapper.readValue("{\"itemCode\":\"ITEM-1\"}", IShippingRequest.class) instanceof ShippingRequest);
    }
}
