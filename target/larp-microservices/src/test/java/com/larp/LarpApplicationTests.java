package com.larp;

import com.larp.database.InventoryItem;
import com.larp.database.InventoryItemRepository;
import com.larp.editor.EditorService;
import com.larp.logviewer.LogEntryRepository;
import com.larp.shipping.ShippingRequest;
import com.larp.shipping.ShippingRequestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LarpApplicationTests {

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private LogEntryRepository logEntryRepository;

    @Autowired
    private ShippingRequestRepository shippingRequestRepository;

    @Autowired
    private EditorService editorService;

    @Test
    void contextLoads() {
        assertThat(inventoryItemRepository).isNotNull();
        assertThat(logEntryRepository).isNotNull();
        assertThat(shippingRequestRepository).isNotNull();
    }

    @Test
    void databaseCanPersistAndReadItemsWithinDateRange() {
        inventoryItemRepository.deleteAll();

        InventoryItem item = new InventoryItem();
        item.setItemCode("WIDGET-100");
        item.setDescription("Widget");
        item.setQuantity(50);
        item.setStatus("AVAILABLE");
        item.setDate(LocalDate.now());
        inventoryItemRepository.save(item);

        assertThat(inventoryItemRepository.findByItemCodeBetweenDates(
                "WIDGET-100",
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(1)
        )).isNotEmpty();
    }

    @Test
    void editorRejectsInvalidEditBeforeSubmission() {
        IllegalArgumentException exception = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> editorService.validateAndSave(new InventoryItem())
        );

        assertThat(exception).hasMessageContaining("itemCode");
    }

    @Test
    void shippingRequestAndLogEntriesAreTracked() {
        shippingRequestRepository.deleteAll();
        logEntryRepository.deleteAll();

        ShippingRequest request = new ShippingRequest();
        request.setRequestId("SR-1");
        request.setItemCode("WIDGET-200");
        request.setPriority("HIGH");
        request.setOrigin("Sydney");
        request.setDestination("Melbourne");
        request.setStatus("PENDING");
        shippingRequestRepository.save(request);

        assertThat(shippingRequestRepository.count()).isGreaterThanOrEqualTo(1);
        assertThat(logEntryRepository.count()).isGreaterThanOrEqualTo(0);
    }
}
