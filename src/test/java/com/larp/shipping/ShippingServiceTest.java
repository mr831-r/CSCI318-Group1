package com.larp.shipping;

import com.larp.database.DatabaseService;
import com.larp.editor.InventoryItem;
import com.larp.events.StockEvent;
import com.larp.logviewer.LogService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShippingServiceTest {

    @Test
    void stockLevelAggregationAddsArrivalsAndSubtractsDispatches() {
        StockEvent arrival = new StockEvent("STOCK_ARRIVED", "ITEM-1", 8, Instant.now());
        StockEvent dispatch = new StockEvent("STOCK_DISPATCHED", "ITEM-1", 3, Instant.now());

        int afterArrival = ShippingService.applyStockEvent(0, arrival);

        assertEquals(8, afterArrival);
        assertEquals(5, ShippingService.applyStockEvent(afterArrival, dispatch));
    }

    @Test
    void confirmArrivalUpdatesInventoryPersistsRequestAndLogsEvent() {
        DatabaseService databaseService = mock(DatabaseService.class);
        LogService logService = mock(LogService.class);
        StreamBridge streamBridge = mock(StreamBridge.class);
        when(streamBridge.send(eq("stockEvents-out-0"), any(Message.class))).thenReturn(true);
        InventoryItem item = new InventoryItem();
        item.setItemCode("ITEM-1");
        item.setStatus("SHIPPING");
        ShippingRequest request = new ShippingRequest();
        request.setItemCode("ITEM-1");
        request.setQuantity(7);
        request.setRequestedDate(LocalDate.of(2026, 9, 16));
        item.setId(1L);
        when(databaseService.updateLatestItem(eq("logistics"), eq("ITEM-1"), any(InventoryItem.class)))
                .thenAnswer(invocation -> {
                    InventoryItem incoming = invocation.getArgument(2);
                    item.updateFrom(incoming);
                    return Optional.of(item);
                });
        when(databaseService.createItem(request)).thenReturn(request);

        Optional<ShippingRequest> result = new ShippingService(databaseService, logService, streamBridge)
                .confirmArrival(request);

        assertTrue(result.isPresent());
        verify(databaseService).updateLatestItem(eq("logistics"), eq("ITEM-1"), any(InventoryItem.class));
        assertEquals("ARRIVED", item.getStatus());
        assertEquals(7, item.getQuantity());
        assertEquals(LocalDate.of(2026, 9, 16), item.getDate());
        verify(databaseService).createItem(request);
        verify(logService).recordShippingConfirmation("ITEM-1");
        ArgumentCaptor<Message<StockEvent>> eventCaptor = ArgumentCaptor.forClass(Message.class);
        verify(streamBridge).send(eq("stockEvents-out-0"), eventCaptor.capture());
        assertEquals("STOCK_ARRIVED", eventCaptor.getValue().getPayload().type());
        assertEquals("ITEM-1", eventCaptor.getValue().getHeaders().get(KafkaHeaders.KEY));
        assertEquals(7, eventCaptor.getValue().getPayload().quantityDelta());
    }
}