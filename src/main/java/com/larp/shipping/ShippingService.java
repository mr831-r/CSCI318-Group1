package com.larp.shipping;

import com.larp.database.DatabaseService;
import com.larp.database.InventoryItem;
import com.larp.logviewer.LogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ShippingService {

    private final DatabaseService databaseService;
    private final LogService logService;

    public ShippingService(DatabaseService databaseService, LogService logService) {
        this.databaseService = databaseService;
        this.logService = logService;
    }

    @Transactional
    public Optional<ShippingRequest> confirmArrival(ShippingRequest request) {
        if (request == null || request.getItemCode() == null || request.getItemCode().isBlank()) {
            return Optional.empty();
        }

        List<InventoryItem> matchingItems = databaseService.findItems(request.getItemCode(), null, null);
        if (!matchingItems.isEmpty()) {
            InventoryItem incoming = new InventoryItem();
            incoming.setStatus("ARRIVED");
            if (request.getQuantity() != null) {
                incoming.setQuantity(request.getQuantity());
            }
            if (request.getRequestedDate() != null) {
                incoming.setDate(request.getRequestedDate());
            }
            databaseService.updateLatestItem(request.getItemCode(), incoming);
        }

        ShippingRequest saved = databaseService.saveShippingRequest(request);
        logService.recordShippingConfirmation(request.getItemCode());
        return Optional.of(saved);
    }

    public List<ShippingRequest> getShippingRequests() {
        return databaseService.getShippingRequests();
    }
}