package com.larp.database;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.metamodel.EntityType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
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
}
