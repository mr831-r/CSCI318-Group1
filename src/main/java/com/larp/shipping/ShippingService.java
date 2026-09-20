package com.larp.shipping;

import com.larp.database.InventoryItem;
import com.larp.database.InventoryItemRepository;
import com.larp.logviewer.LogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ShippingService {

    private final ShippingRequestRepository shippingRequestRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final LogService logService;

    public ShippingService(ShippingRequestRepository shippingRequestRepository,
                           InventoryItemRepository inventoryItemRepository,
                           LogService logService) {
        this.shippingRequestRepository = shippingRequestRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.logService = logService;
    }

    @Transactional
    public Optional<ShippingRequest> confirmArrival(ShippingRequest request) {
        if (request == null || request.getItemCode() == null || request.getItemCode().isBlank()) {
            return Optional.empty();
        }

        List<InventoryItem> matchingItems = inventoryItemRepository.findByItemCode(request.getItemCode());
        if (!matchingItems.isEmpty()) {
            InventoryItem latestItem = matchingItems.get(matchingItems.size() - 1);
            latestItem.setStatus("ARRIVED");
            if (request.getQuantity() != null) {
                latestItem.setQuantity(request.getQuantity());
            }
            if (request.getRequestedDate() != null) {
                latestItem.setDate(request.getRequestedDate());
            }
            inventoryItemRepository.save(latestItem);
        }

        ShippingRequest saved = shippingRequestRepository.save(request);
        logService.recordShippingConfirmation(request.getItemCode());
        return Optional.of(saved);
    }

    public List<ShippingRequest> getShippingRequests() {
        return shippingRequestRepository.findAll();
    }
}