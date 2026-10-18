package com.larp.shipping;

import com.larp.database.DatabaseService;
import com.larp.database.IDatabaseItem;
import com.larp.editor.InventoryItem;
import com.larp.editor.InventoryStatus;
import com.larp.logviewer.LogService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class ShippingServiceTest {

    @Test
    void confirmArrivalUpdatesInventoryPersistsRequestAndLogsEvent() {
        DatabaseService databaseService = mock(DatabaseService.class);
        LogService logService = mock(LogService.class);
        StreamBridge streamBridge = mock(StreamBridge.class);
        when(streamBridge.send(eq("stockEvents-out-0"), any(Message.class))).thenReturn(true);
        InventoryItem item = new InventoryItem();
        item.setItemCode("ITEM-1");
        item.setQuantity(4);
        item.setStatus(InventoryStatus.SHIPPING);
        item.setDate(LocalDate.of(2026, 9, 10));
        ShippingRequest request = new ShippingRequest();
        request.setItemCode("ITEM-1");
        request.setQuantity(7);
        request.setStatus(ShippingStatus.PENDING);
        request.setDestination("North Camp");
        request.setRequestedDate(LocalDate.of(2026, 9, 16));
        item.setId(1L);
        when(databaseService.mutateLatestItem(eq("logistics"), eq("ITEM-1"), any()))
                .thenAnswer(invocation -> {
                    Consumer<IDatabaseItem> update = invocation.getArgument(2);
                    update.accept(item);
                    return Optional.of(item);
                });
        when(databaseService.createItem(request)).thenReturn(request);

        Optional<ShippingRequest> result = new ShippingService(databaseService, logService, streamBridge)
                .confirmArrival(request);

        assertTrue(result.isPresent());
        verify(databaseService).mutateLatestItem(eq("logistics"), eq("ITEM-1"), any());
        assertEquals(InventoryStatus.ARRIVED, item.getStatus());
        assertEquals(11, item.getQuantity());
        assertEquals(LocalDate.of(2026, 9, 16), item.getDate());
        assertEquals(ShippingStatus.ARRIVED, request.getStatus());
        verify(databaseService).createItem(request);
        verify(logService).recordShippingConfirmation("ITEM-1");
        ArgumentCaptor<Message<StockEvent>> eventCaptor = ArgumentCaptor.forClass(Message.class);
        verify(streamBridge).send(eq("stockEvents-out-0"), eventCaptor.capture());
        assertEquals("STOCK_ARRIVED", eventCaptor.getValue().getPayload().type());
        assertEquals("ITEM-1", eventCaptor.getValue().getHeaders().get(KafkaHeaders.KEY));
        assertEquals(7, eventCaptor.getValue().getPayload().quantityDelta());
    }

    @Test
    void confirmArrivalReportsUnknownInventoryItemAsInvalid() {
        DatabaseService databaseService = mock(DatabaseService.class);
        LogService logService = mock(LogService.class);
        StreamBridge streamBridge = mock(StreamBridge.class);
        ShippingRequest request = new ShippingRequest();
        request.setItemCode("MISSING");
        request.setQuantity(3);
        request.setDestination("Test destination");
        request.setRequestedDate(LocalDate.of(2026, 9, 16));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> new ShippingService(databaseService, logService, streamBridge).confirmArrival(request));

        assertEquals(400, exception.getStatusCode().value());
        verify(databaseService).mutateLatestItem(eq("logistics"), eq("MISSING"), any());
        verify(databaseService, never()).createItem(any());
        verify(logService, never()).recordShippingConfirmation(any());
        verify(streamBridge, never()).send(any(String.class), any(Object.class));
    }
}