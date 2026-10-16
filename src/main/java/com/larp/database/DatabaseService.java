package com.larp.database;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Service
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

    public <T extends IDatabaseItem> List<T> findItems(Class<T> itemType, String filter) {
        return findItems(itemType, filter, null, null);
    }

    public <T extends IDatabaseItem> List<T> findItems(
            Class<T> itemType,
            String filter,
            LocalDate start,
            LocalDate end) {
        return findAllItems(itemType).stream()
                .filter(item -> filter == null || filter.isBlank()
                        || filter.equals(item.getItemCode())
                        || filter.equals(item.getFilterName()))
                .filter(item -> start == null || (item.getDate() != null && !item.getDate().isBefore(start)))
                .filter(item -> end == null || (item.getDate() != null && !item.getDate().isAfter(end)))
                .sorted(Comparator
                        .comparing(IDatabaseItem::getDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(IDatabaseItem::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public <T extends IDatabaseItem> Optional<T> findLatestItem(Class<T> itemType, String filter) {
        return findItems(itemType, filter).stream().findFirst();
    }

    @Transactional
    public <T extends IDatabaseItem> Optional<T> updateItemFields(
            Class<T> itemType,
            Long id,
            Consumer<T> updater) {
        if (id == null) {
            return Optional.empty();
        }
        T item = em.find(itemType, id);
        if (item == null) {
            return Optional.empty();
        }
        updater.accept(item);
        return Optional.of(em.merge(item));
    }

    @Transactional
    public <T extends IDatabaseItem> Optional<T> updateItem(Class<T> itemType, Long id, T incoming) {
        if (incoming == null) {
            throw new IllegalArgumentException("Incoming database item cannot be null");
        }
        return updateItemFields(itemType, id, item -> item.updateFrom(incoming));
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
}
