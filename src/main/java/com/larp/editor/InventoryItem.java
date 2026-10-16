package com.larp.editor;

import com.larp.database.IDatabaseItem;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "inventory_items")
public class InventoryItem implements IDatabaseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String itemCode;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private LocalDate date;

    @Lob
    private String payslipReference;

    public InventoryItem() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getFilterName() {
        return "logistics";
    }

    @Override
    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getPayslipReference() {
        return payslipReference;
    }

    public void setPayslipReference(String payslipReference) {
        this.payslipReference = payslipReference;
    }

    @Override
    public void updateFrom(IDatabaseItem incoming) {
        if (!(incoming instanceof InventoryItem item)) {
            throw new IllegalArgumentException("Inventory items can only be updated from inventory items");
        }
        if (item.getItemCode() != null) {
            setItemCode(item.getItemCode());
        }
        if (item.getName() != null) {
            setName(item.getName());
        }
        if (item.getDescription() != null) {
            setDescription(item.getDescription());
        }
        if (item.getQuantity() != null) {
            setQuantity(item.getQuantity());
        }
        if (item.getStatus() != null) {
            setStatus(item.getStatus());
        }
        if (item.getDate() != null) {
            setDate(item.getDate());
        }
        if (item.getPayslipReference() != null) {
            setPayslipReference(item.getPayslipReference());
        }
    }
}
