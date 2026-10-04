package com.larp.shipping;

import com.larp.database.DatabaseService;
import com.larp.database.InventoryItem;
import com.larp.logviewer.LogService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShippingServiceTest {

    @Test
    void confirmArrivalUpdatesInventoryPersistsRequestAndLogsEvent() {
        DatabaseService databaseService = mock(DatabaseService.class);
        LogService logService = mock(LogService.class);
        InventoryItem item = new InventoryItem();
        item.setItemCode("ITEM-1");
        item.setStatus("SHIPPING");
        ShippingRequest request = new ShippingRequest();
        request.setItemCode("ITEM-1");
        request.setQuantity(7);
        request.setRequestedDate(LocalDate.of(2026, 9, 16));
        when(databaseService.findItems("ITEM-1", null, null)).thenReturn(List.of(item));
        when(databaseService.saveShippingRequest(request)).thenReturn(request);

        Optional<ShippingRequest> result = new ShippingService(databaseService, logService)
                .confirmArrival(request);

        assertTrue(result.isPresent());
        assertEquals("ARRIVED", databaseService.findItems("ITEM-1", null, null).get(0).getStatus());
        assertEquals(7, databaseService.findItems("ITEM-1", null, null).get(0).getQuantity());
        assertEquals(LocalDate.of(2026, 9, 16), databaseService.findItems("ITEM-1", null, null).get(0).getDate());
        verify(databaseService).updateLatestItem(any(String.class), any(InventoryItem.class));
        verify(databaseService).saveShippingRequest(request);
        verify(logService).recordShippingConfirmation("ITEM-1");
    }
}