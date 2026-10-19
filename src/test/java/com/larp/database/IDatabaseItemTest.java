package com.larp.database;

import com.fasterxml.jackson.databind.ObjectMapper;
<<<<<<< HEAD
import com.larp.ai.TermAlias;
=======
>>>>>>> 3aee914b11d4fe54347f4e181b6413330c75b1b7
import com.larp.logviewer.LogEntry;
import com.larp.shipping.ShippingRequest;
import com.larp.editor.InventoryItem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class IDatabaseItemTest {

    @Test
    void entitiesImplementIDatabaseItem() {
        assertTrue(IDatabaseItem.class.isAssignableFrom(InventoryItem.class), "InventoryItem should implement IDatabaseItem");
        assertTrue(IDatabaseItem.class.isAssignableFrom(LogEntry.class), "LogEntry should implement IDatabaseItem");
        assertTrue(IDatabaseItem.class.isAssignableFrom(ShippingRequest.class), "ShippingRequest should implement IDatabaseItem");
    }

    @Test
    void genericDatabaseItemsDeserializeIntoTheirImplementations() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        assertTrue(mapper.readValue("{\"name\":\"Sword\"}", IDatabaseItem.class) instanceof InventoryItem);
        assertTrue(mapper.readValue("{\"message\":\"Saved\"}", IDatabaseItem.class) instanceof LogEntry);
        assertTrue(mapper.readValue("{\"destination\":\"North Camp\"}", IDatabaseItem.class) instanceof ShippingRequest);
<<<<<<< HEAD
        assertTrue(mapper.readValue("{\"term\":\"blade\",\"meaning\":\"sword\"}", IDatabaseItem.class) instanceof TermAlias);
=======
>>>>>>> 3aee914b11d4fe54347f4e181b6413330c75b1b7
    }
}
