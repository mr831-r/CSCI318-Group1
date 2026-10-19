package com.larp.database;

import com.larp.editor.InventoryItem;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class DatabaseServiceEndpointTest {

    @Test
    void updateEndpointUsesIncomingItemFilterToSelectEntityType() {
        DatabaseService databaseService = spy(new DatabaseService());
        InventoryItem incoming = new InventoryItem();
        incoming.setItemCode("ITEM-1");
        doReturn(Optional.of(incoming))
                .when(databaseService)
                .updateLatestItem("logistics", "ITEM-1", incoming);

        IDatabaseItem updated = databaseService.updateLatestItemEndpoint("ITEM-1", incoming);

        assertSame(incoming, updated);
        verify(databaseService).updateLatestItem("logistics", "ITEM-1", incoming);
    }
}
