package com.larp.shipping;

import com.larp.database.InventoryItem;
import com.larp.database.InventoryItemRepository;
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
        ShippingRequestRepository shippingRepository = mock(ShippingRequestRepository.class);
        InventoryItemRepository inventoryRepository = mock(InventoryItemRepository.class);
        LogService logService = mock(LogService.class);
        InventoryItem item = new InventoryItem();
        item.setItemCode("ITEM-1");
        item.setStatus("SHIPPING");
        ShippingRequest request = new ShippingRequest();
        request.setItemCode("ITEM-1");
        request.setQuantity(7);
        request.setRequestedDate(LocalDate.of(2026, 9, 16));
        when(inventoryRepository.findByItemCode("ITEM-1")).thenReturn(List.of(item));
        when(shippingRepository.save(request)).thenReturn(request);

        Optional<ShippingRequest> result = new ShippingService(shippingRepository, inventoryRepository, logService)
                .confirmArrival(request);

        assertTrue(result.isPresent());
        assertEquals("ARRIVED", item.getStatus());
        assertEquals(7, item.getQuantity());
        assertEquals(LocalDate.of(2026, 9, 16), item.getDate());
        verify(inventoryRepository).save(item);
        verify(shippingRepository).save(request);
        verify(logService).recordShippingConfirmation("ITEM-1");
    }
}