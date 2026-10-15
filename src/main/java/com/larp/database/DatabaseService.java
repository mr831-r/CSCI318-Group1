package com.larp.database;

import com.larp.logviewer.LogEntry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RestController
@RequestMapping("/database")
public class DatabaseService {

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public <T extends IDatabaseItem> T createItem(T item) {
        if (item == null) {
            throw new IllegalArgumentException("Database item cannot be null");
        }
        return em.merge(item);
    }

    public <T extends IDatabaseItem> List<T> findAllItems(Class<T> itemType) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<T> query = builder.createQuery(itemType);
        Root<T> item = query.from(itemType);
        query.select(item);
        return em.createQuery(query).getResultList();
    }

    public <T extends IDatabaseItem> Optional<T> findItem(Class<T> itemType, Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(em.find(itemType, id));
    }

    public <T extends IDatabaseItem> List<T> findItems(Class<T> itemType, String itemCode) {
        return findAllItems(itemType).stream()
                .filter(item -> itemCode == null || itemCode.equals(item.getItemCode()))
                .toList();
    }

    @Transactional
    public <T extends IDatabaseItem> boolean deleteItem(Class<T> itemType, Long id) {
        if (id == null) {
            return false;
        }
        T item = em.find(itemType, id);
        if (item == null) {
            return false;
        }
        em.remove(item);
        return true;
    }

    public List<InventoryItem> findItems(String filter, LocalDate start, LocalDate end) {
        List<InventoryItem> matchingItems = findItems(InventoryItem.class, filter);
        if (start != null && end != null) {
            List<InventoryItem> history = matchingItems.stream()
                    .filter(item -> !item.getDate().isBefore(start) && !item.getDate().isAfter(end))
                    .sorted(Comparator.comparing(InventoryItem::getDate).reversed())
                    .toList();
            if (!history.isEmpty()) {
                return history;
            }
        }

        return matchingItems.stream()
                .sorted(Comparator.comparing(InventoryItem::getDate))
                .toList();
    }

    public List<InventoryItem> findAllItems() {
        return findAllItems(InventoryItem.class);
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

        InventoryItem merged = createItem(existing);
        return Optional.of(merged);
    }

    @Transactional
    public boolean deleteLatestItem(String filter) {
        List<InventoryItem> items = findItems(filter, null, null);
        if (items.isEmpty()) {
            return false;
        }

        InventoryItem toRemove = items.get(items.size() - 1);
        return deleteItem(InventoryItem.class, toRemove.getId());
    }

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
    @Transactional
    public InventoryItem updateItemEndpoint(@PathVariable String filter, @RequestBody InventoryItem incoming) {
        return updateLatestItem(filter, incoming).orElse(null);
    }

    @DeleteMapping("/{filter}")
    public void deleteItemEndpoint(@PathVariable String filter) {
        deleteLatestItem(filter);
    }

    @Transactional
    @PutMapping("/logs")
    public LogEntry createLogEntryEndpoint(@RequestBody LogEntry entry) {
        return createItem(entry);
    }
}
