package com.larp.database;

import com.larp.logviewer.LogEntry;
import com.larp.shipping.ShippingRequest;
import com.larp.editor.InventoryItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.client.RestClient;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.cloud.function.definition=",
        "spring.cloud.stream.function.autodetect=false"
})
@Transactional
class DatabaseServiceTest {

    @Autowired
    private DatabaseService databaseService;

    @Value("${local.server.port}")
    private int serverPort;

    @Test
    @DirtiesContext
    void restEndpointPersistsKafkaConsumerLogEntry() throws Exception {
        LogEntry entry = new LogEntry();
        entry.setMessage("ITEM_ADDED for item ITEM-1");
        entry.setLevel("INFO");
        entry.setTimestamp(LocalDateTime.of(2026, 10, 14, 10, 0));

        LogEntry result = RestClient.builder()
                .baseUrl("http://localhost:" + serverPort)
                .build()
                .put()
                .uri("/database/logs")
                .body(entry)
                .retrieve()
                .body(LogEntry.class);

        assertEquals("ITEM_ADDED for item ITEM-1", result.getMessage());
    }

    @Test
    void genericCrudPersistsEachDatabaseItemType() {
        InventoryItem inventoryItem = new InventoryItem();
        inventoryItem.setItemCode("ITEM-DB-1");
        inventoryItem.setName("Test item");
        inventoryItem.setDescription("Database persistence test");
        inventoryItem.setQuantity(2);
        inventoryItem.setStatus("AVAILABLE");
        inventoryItem.setDate(LocalDate.of(2026, 10, 14));

        LogEntry logEntry = new LogEntry();
        logEntry.setMessage("Database persistence test");
        logEntry.setLevel("INFO");
        logEntry.setTimestamp(LocalDateTime.of(2026, 10, 14, 10, 0));

        ShippingRequest shippingRequest = new ShippingRequest();
        shippingRequest.setItemCode("ITEM-DB-1");
        shippingRequest.setDestination("Test destination");
        shippingRequest.setQuantity(2);
        shippingRequest.setStatus("PENDING");
        shippingRequest.setRequestedDate(LocalDate.of(2026, 10, 14));

        InventoryItem savedInventory = InventoryItem.class.cast(databaseService.createItem(inventoryItem));
        LogEntry savedLog = LogEntry.class.cast(databaseService.createItem(logEntry));
        ShippingRequest savedShipping = ShippingRequest.class.cast(databaseService.createItem(shippingRequest));

        assertTrue(databaseService.findItem("logistics", savedInventory.getId()).isPresent());
        assertTrue(databaseService.findItem("log", savedLog.getId()).isPresent());
        assertTrue(databaseService.findItem("shipping", savedShipping.getId()).isPresent());
        assertEquals(1, databaseService.findItems("logistics", null, null, null).size());
        assertEquals(1, databaseService.findItems("log", null, null, null).size());
        assertEquals(1, databaseService.findItems("shipping", null, null, null).size());

        assertTrue(databaseService.deleteItem(savedShipping));
        assertTrue(databaseService.findItem("shipping", savedShipping.getId()).isEmpty());
    }

    @Test
    void genericQueriesUpdatesAndDeletesInventoryRecords() {
        InventoryItem older = inventoryItem("ITEM-GENERIC", LocalDate.of(2026, 10, 13), 2);
        InventoryItem latest = inventoryItem("ITEM-GENERIC", LocalDate.of(2026, 10, 14), 5);
        databaseService.createItem(older);
        latest = InventoryItem.class.cast(databaseService.createItem(latest));

        List<InventoryItem> history = databaseService.findItems(
                "logistics",
                "ITEM-GENERIC",
                LocalDate.of(2026, 10, 14),
                LocalDate.of(2026, 10, 14)).stream()
                .map(InventoryItem.class::cast)
                .toList();
        assertEquals(List.of(latest), history);
        assertEquals(
                latest,
                InventoryItem.class.cast(databaseService.findLatestItem("logistics", "ITEM-GENERIC").orElseThrow()));

        InventoryItem update = new InventoryItem();
        update.setQuantity(8);
        update.setStatus("ARRIVED");
        InventoryItem updated = InventoryItem.class.cast(databaseService.updateLatestItem(
                "logistics",
                "ITEM-GENERIC",
                update).orElseThrow());
        assertEquals(8, updated.getQuantity());
        assertEquals("ARRIVED", updated.getStatus());
        assertEquals("Test item", updated.getName());

        assertTrue(databaseService.deleteItem(updated));
        assertTrue(databaseService.findItem("logistics", updated.getId()).isEmpty());
    }

    private InventoryItem inventoryItem(String itemCode, LocalDate date, int quantity) {
        InventoryItem item = new InventoryItem();
        item.setItemCode(itemCode);
        item.setName("Test item");
        item.setDescription("Generic database operation test");
        item.setQuantity(quantity);
        item.setStatus("AVAILABLE");
        item.setDate(date);
        return item;
    }
}
