package com.larp.database;

import com.larp.logviewer.LogEntry;
import com.larp.shipping.ShippingRequest;
import com.larp.shipping.ShippingRequestRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RestController
@RequestMapping("/database")
public class DatabaseService {

    @PersistenceContext
    private EntityManager em;

    private final ShippingRequestRepository shippingRequestRepository;

    public DatabaseService(ShippingRequestRepository shippingRequestRepository) {
        this.shippingRequestRepository = shippingRequestRepository;
    }

    // Inventory operations
    @Transactional
    public InventoryItem createItem(InventoryItem item) {
        // Use merge so method works for both new and detached entities
        InventoryItem merged = em.merge(item);
        return merged;
    }

    public List<InventoryItem> findItems(String filter, LocalDate start, LocalDate end) {
        if (start != null && end != null) {
            TypedQuery<InventoryItem> q = em.createQuery("SELECT i FROM InventoryItem i WHERE i.itemCode = :itemCode AND i.date BETWEEN :start AND :end ORDER BY i.date DESC", InventoryItem.class);
            q.setParameter("itemCode", filter);
            q.setParameter("start", start);
            q.setParameter("end", end);
            List<InventoryItem> history = q.getResultList();
            if (!history.isEmpty()) {
                return history;
            }
        }

        TypedQuery<InventoryItem> q = em.createQuery("SELECT i FROM InventoryItem i WHERE i.itemCode = :itemCode ORDER BY i.date ASC", InventoryItem.class);
        q.setParameter("itemCode", filter);
        return q.getResultList();
    }

    public List<InventoryItem> findAllItems() {
        TypedQuery<InventoryItem> q = em.createQuery("SELECT i FROM InventoryItem i", InventoryItem.class);
        return q.getResultList();
    }

    public List<InventoryItem> findByDateBetween(LocalDate start, LocalDate end) {
        TypedQuery<InventoryItem> q = em.createQuery("SELECT i FROM InventoryItem i WHERE i.date BETWEEN :start AND :end", InventoryItem.class);
        q.setParameter("start", start);
        q.setParameter("end", end);
        return q.getResultList();
    }

    public List<InventoryItem> findByDateGreaterThanEqual(LocalDate start) {
        TypedQuery<InventoryItem> q = em.createQuery("SELECT i FROM InventoryItem i WHERE i.date >= :start", InventoryItem.class);
        q.setParameter("start", start);
        return q.getResultList();
    }

    public List<InventoryItem> findByDateLessThanEqual(LocalDate end) {
        TypedQuery<InventoryItem> q = em.createQuery("SELECT i FROM InventoryItem i WHERE i.date <= :end", InventoryItem.class);
        q.setParameter("end", end);
        return q.getResultList();
    }

    public List<InventoryItem> findByDescriptionContainingIgnoreCase(String description) {
        TypedQuery<InventoryItem> q = em.createQuery("SELECT i FROM InventoryItem i WHERE LOWER(i.description) LIKE LOWER(CONCAT('%', :desc, '%')) OR LOWER(i.name) LIKE LOWER(CONCAT('%', :desc, '%'))", InventoryItem.class);
        q.setParameter("desc", description);
        return q.getResultList();
    }

    @Transactional
    public Optional<InventoryItem> updateLatestItem(String filter, InventoryItem incoming) {
        List<InventoryItem> existingItems = findItems(filter, null, null);
        if (existingItems.isEmpty()) {
            return Optional.empty();
        }

        InventoryItem existing = existingItems.get(existingItems.size() - 1);
        existing.setDescription(incoming.getDescription() != null ? incoming.getDescription() : existing.getDescription());
        existing.setQuantity(incoming.getQuantity() != null ? incoming.getQuantity() : existing.getQuantity());
        existing.setStatus(incoming.getStatus() != null ? incoming.getStatus() : existing.getStatus());
        existing.setDate(incoming.getDate() != null ? incoming.getDate() : existing.getDate());
        existing.setPayslipReference(incoming.getPayslipReference() != null ? incoming.getPayslipReference() : existing.getPayslipReference());

        InventoryItem merged = em.merge(existing);
        return Optional.of(merged);
    }

    @Transactional
    public boolean deleteLatestItem(String filter) {
        List<InventoryItem> items = findItems(filter, null, null);
        if (items.isEmpty()) {
            return false;
        }

        InventoryItem toRemove = items.get(items.size() - 1);
        InventoryItem managed = em.contains(toRemove) ? toRemove : em.merge(toRemove);
        em.remove(managed);
        return true;
    }

    // Log operations
    public List<LogEntry> findLogs(LocalDate start, LocalDate end) {
        if (start != null && end != null) {
            TypedQuery<LogEntry> q = em.createQuery("SELECT l FROM LogEntry l WHERE l.timestamp BETWEEN :start AND :end ORDER BY l.timestamp DESC", LogEntry.class);
            q.setParameter("start", start.atStartOfDay());
            q.setParameter("end", end.atTime(23, 59, 59));
            return q.getResultList();
        }
        TypedQuery<LogEntry> q = em.createQuery("SELECT l FROM LogEntry l ORDER BY l.timestamp DESC", LogEntry.class);
        return q.getResultList();
    }

    @Transactional
    public LogEntry saveLogEntry(LogEntry entry) {
        if (entry.getId() == null) {
            em.persist(entry);
            return entry;
        }
        return em.merge(entry);
    }

    // Shipping operations
    public ShippingRequest saveShippingRequest(ShippingRequest request) {
        return shippingRequestRepository.save(request);
    }

    public List<ShippingRequest> getShippingRequests() {
        return shippingRequestRepository.findAll();
    }

    /*
     * Controller endpoints (moved from DatabaseController into this service class)
     */
    @PutMapping
    public InventoryItem createItemEndpoint(@RequestBody InventoryItem item) {
        return createItem(item);
    }

    @PutMapping("/{filter}")
    public InventoryItem createItemWithFilterEndpoint(@PathVariable String filter, @RequestBody InventoryItem item) {
        if (item == null) {
            item = new InventoryItem();
        }
        if (item.getItemCode() == null || item.getItemCode().isBlank()) {
            item.setItemCode(filter);
        }
        return createItem(item);
    }

    @GetMapping("/{filter}")
    public List<InventoryItem> readItemsEndpoint(
            @PathVariable String filter,
            @RequestParam(value = "start", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(value = "end", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return findItems(filter, start, end);
    }

    @PostMapping("/{filter}")
    public InventoryItem updateItemEndpoint(@PathVariable String filter, @RequestBody InventoryItem incoming) {
        return updateLatestItem(filter, incoming).orElse(null);
    }

    @DeleteMapping("/{filter}")
    public void deleteItemEndpoint(@PathVariable String filter) {
        deleteLatestItem(filter);
    }
}
