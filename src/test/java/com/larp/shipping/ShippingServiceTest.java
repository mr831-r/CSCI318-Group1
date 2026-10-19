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
import java.util.List;
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

    @Test
    void createRequestStoresPendingRequestAndPublishesEvent() {
        DatabaseService databaseService = mock(DatabaseService.class);
        StreamBridge streamBridge = mock(StreamBridge.class);
        when(streamBridge.send(eq("stockEvents-out-0"), any(Message.class))).thenReturn(true);
        when(databaseService.findLatestItem("logistics", "ITEM-1")).thenReturn(Optional.of(new InventoryItem()));
        when(databaseService.createItem(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ShippingRequest request = new ShippingRequest();
        request.setItemCode("ITEM-1");
        request.setQuantity(3);
        request.setDestination("North Camp");
        request.setStatus(ShippingStatus.ARRIVED); // ignored: new requests are always PENDING

        ShippingRequest saved = new ShippingService(databaseService, mock(LogService.class), streamBridge)
                .createRequest(request);

        assertEquals(ShippingStatus.PENDING, saved.getStatus());
        assertEquals(LocalDate.now(), saved.getRequestedDate());
        ArgumentCaptor<Message<StockEvent>> eventCaptor = ArgumentCaptor.forClass(Message.class);
        verify(streamBridge).send(eq("stockEvents-out-0"), eventCaptor.capture());
        assertEquals("SHIPPING_REQUESTED", eventCaptor.getValue().getPayload().type());
    }

    @Test
    void dispatchTakesStockAssignsCourierAndPublishesNegativeDelta() {
        DatabaseService databaseService = mock(DatabaseService.class);
        StreamBridge streamBridge = mock(StreamBridge.class);
        when(streamBridge.send(eq("stockEvents-out-0"), any(Message.class))).thenReturn(true);
        InventoryItem item = new InventoryItem();
        item.setItemCode("ITEM-1");
        item.setQuantity(10);
        ShippingRequest request = pendingRequest(5L, "ITEM-1", 4);
        stubDispatchLookups(databaseService, item, request);

        ShippingRequest dispatched = new ShippingService(databaseService, mock(LogService.class), streamBridge)
                .dispatch(5L, "Fast Horse");

        assertEquals(6, item.getQuantity());
        assertEquals(ShippingStatus.DISPATCHED, dispatched.getStatus());
        assertEquals("Fast Horse", dispatched.getCourier());
        ArgumentCaptor<Message<StockEvent>> eventCaptor = ArgumentCaptor.forClass(Message.class);
        verify(streamBridge).send(eq("stockEvents-out-0"), eventCaptor.capture());
        assertEquals("STOCK_DISPATCHED", eventCaptor.getValue().getPayload().type());
        assertEquals(-4, eventCaptor.getValue().getPayload().quantityDelta());
    }

    @Test
    void dispatchRefusesWhenThereIsNotEnoughStock() {
        DatabaseService databaseService = mock(DatabaseService.class);
        StreamBridge streamBridge = mock(StreamBridge.class);
        InventoryItem item = new InventoryItem();
        item.setItemCode("ITEM-1");
        item.setQuantity(2);
        ShippingRequest request = pendingRequest(5L, "ITEM-1", 4);
        stubDispatchLookups(databaseService, item, request);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> new ShippingService(databaseService, mock(LogService.class), streamBridge).dispatch(5L, "Fast Horse"));

        assertEquals(409, exception.getStatusCode().value());
        assertEquals(2, item.getQuantity());
        assertEquals(ShippingStatus.PENDING, request.getStatus());
        verify(streamBridge, never()).send(any(String.class), any(Object.class));
    }

    @Test
    void requestQueueListsOpenRequestsOldestFirst() {
        DatabaseService databaseService = mock(DatabaseService.class);
        ShippingRequest newer = pendingRequest(1L, "ITEM-1", 1);
        newer.setRequestedDate(LocalDate.of(2026, 9, 5));
        ShippingRequest older = pendingRequest(2L, "ITEM-2", 1);
        older.setRequestedDate(LocalDate.of(2026, 9, 1));
        ShippingRequest arrival = pendingRequest(3L, "ITEM-3", 1);
        arrival.setStatus(ShippingStatus.ARRIVED);
        when(databaseService.findItems("shipping", null, null, null)).thenReturn(List.of(newer, arrival, older));

        List<ShippingRequest> queue = new ShippingService(databaseService, mock(LogService.class), mock(StreamBridge.class))
                .getRequestQueue(null);

        assertEquals(List.of(older, newer), queue);
    }

    private static ShippingRequest pendingRequest(Long id, String code, int quantity) {
        ShippingRequest request = new ShippingRequest();
        request.setId(id);
        request.setItemCode(code);
        request.setQuantity(quantity);
        request.setDestination("North Camp");
        request.setRequestedDate(LocalDate.of(2026, 9, 1));
        request.setStatus(ShippingStatus.PENDING);
        return request;
    }

    private static void stubDispatchLookups(DatabaseService databaseService, InventoryItem item, ShippingRequest request) {
        when(databaseService.findItem("shipping", request.getId())).thenReturn(Optional.of(request));
        when(databaseService.mutateLatestItem(eq("logistics"), eq(item.getItemCode()), any())).thenAnswer(invocation -> {
            Consumer<IDatabaseItem> update = invocation.getArgument(2);
            update.accept(item);
            return Optional.of(item);
        });
        when(databaseService.mutateItem(eq("shipping"), eq(request.getId()), any())).thenAnswer(invocation -> {
            Consumer<IDatabaseItem> update = invocation.getArgument(2);
            update.accept(request);
            return Optional.of(request);
        });
    }
}
