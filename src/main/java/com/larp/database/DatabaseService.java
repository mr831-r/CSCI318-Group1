package com.larp.database;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.metamodel.EntityType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
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
    public IDatabaseItem createItem(IDatabaseItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Database item cannot be null");
        }
        Object saved = em.merge(item);
        if (!(saved instanceof IDatabaseItem databaseItem)) {
            throw new IllegalStateException("Persisted entity does not implement IDatabaseItem");
        }
        return databaseItem;
    }

    public List<IDatabaseItem> findAllItems() {
        List<IDatabaseItem> items = new ArrayList<>();
        for (EntityType<?> entityType : em.getMetamodel().getEntities()) {
            if (!IDatabaseItem.class.isAssignableFrom(entityType.getJavaType())) {
                continue;
            }
            String queryText = "SELECT item FROM " + entityType.getName() + " item";
            List<?> results = em.createQuery(queryText, Object.class).getResultList();
            for (Object result : results) {
                if (result instanceof IDatabaseItem item) {
                    items.add(item);
                }
            }
        }
        return items.stream()
                .sorted(Comparator
                        .comparing(IDatabaseItem::getDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(IDatabaseItem::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public List<IDatabaseItem> findItems(
            String filterName,
            String itemCode,
            LocalDate start,
            LocalDate end) {
        return findAllItems().stream()
                .filter(item -> filterName == null || filterName.isBlank()
                        || filterName.equals(item.getFilterName()))
                .filter(item -> itemCode == null || itemCode.isBlank()
                        || itemCode.equals(item.getItemCode()))
                .filter(item -> start == null || (item.getDate() != null && !item.getDate().isBefore(start)))
                .filter(item -> end == null || (item.getDate() != null && !item.getDate().isAfter(end)))
                .toList();
    }

    public List<IDatabaseItem> findItemsByFilter(String filter, LocalDate start, LocalDate end) {
        return findAllItems().stream()
                .filter(item -> filter == null || filter.isBlank()
                        || filter.equals(item.getFilterName())
                        || filter.equals(item.getItemCode()))
                .filter(item -> start == null || (item.getDate() != null && !item.getDate().isBefore(start)))
                .filter(item -> end == null || (item.getDate() != null && !item.getDate().isAfter(end)))
                .toList();
    }

    public Optional<IDatabaseItem> findItem(String filterName, Long id) {
        if (filterName == null || id == null) {
            return Optional.empty();
        }
        return findItems(filterName, null, null, null).stream()
                .filter(item -> id.equals(item.getId()))
                .findFirst();
    }

    public Optional<IDatabaseItem> findLatestItem(String filterName, String itemCode) {
        return findItems(filterName, itemCode, null, null).stream().findFirst();
    }

    @Transactional
    public Optional<IDatabaseItem> updateLatestItem(
            String filterName,
            String itemCode,
            IDatabaseItem incoming) {
        if (incoming == null) {
            throw new IllegalArgumentException("Incoming database item cannot be null");
        }
        Optional<IDatabaseItem> existing = findLatestItem(filterName, itemCode);
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        IDatabaseItem item = existing.get();
        item.updateFrom(incoming);
        Object saved = em.merge(item);
        if (!(saved instanceof IDatabaseItem databaseItem)) {
            throw new IllegalStateException("Updated entity does not implement IDatabaseItem");
        }
        return Optional.of(databaseItem);
    }

    @Transactional
    public boolean deleteItem(IDatabaseItem item) {
        if (item == null || item.getId() == null) {
            return false;
        }
        Object persisted = em.find(item.getClass(), item.getId());
        if (persisted == null) {
            return false;
        }
        em.remove(persisted);
        return true;
    }

    @Transactional
    public boolean deleteLatestItem(String filterName, String itemCode) {
        return findLatestItem(filterName, itemCode)
                .map(this::deleteItem)
                .orElse(false);
    }

    @PutMapping
    @Transactional
    public IDatabaseItem createItemEndpoint(@RequestBody IDatabaseItem item) {
        return createItem(item);
    }

    @PutMapping("/{filter}")
    @Transactional
    public IDatabaseItem createItemWithFilterEndpoint(
            @PathVariable String filter,
            @RequestBody IDatabaseItem item) {
        return createItem(item);
    }

    @GetMapping("/{filter}")
    public List<IDatabaseItem> readItemsEndpoint(
            @PathVariable String filter,
            @RequestParam(value = "start", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(value = "end", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return findItemsByFilter(filter, start, end);
    }

    @PostMapping("/{filter}")
    @Transactional
    public IDatabaseItem updateLatestItemEndpoint(
            @PathVariable String filter,
            @RequestBody IDatabaseItem incoming) {
        return updateLatestItem(incoming.getFilterName(), filter, incoming).orElse(null);
    }

    @DeleteMapping("/{filter}")
    public void deleteLatestItemEndpoint(@PathVariable String filter) {
        deleteLatestItem(null, filter);
    }
}
