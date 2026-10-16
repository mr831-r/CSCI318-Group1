package com.larp.shipping;

import com.larp.database.IDatabaseItem;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "shipping_requests")
public class ShippingRequest implements IDatabaseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String itemCode;

    @Column(nullable = false)
    private String destination;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private LocalDate requestedDate;

    public ShippingRequest() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getFilterName() {
        return "shipping";
    }

    @Override
    public String getItemCode() {
        return itemCode;
    }

    @Override
    public LocalDate getDate() {
        return requestedDate;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
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

    public LocalDate getRequestedDate() {
        return requestedDate;
    }

    public void setRequestedDate(LocalDate requestedDate) {
        this.requestedDate = requestedDate;
    }

    @Override
    public void updateFrom(IDatabaseItem incoming) {
        if (!(incoming instanceof ShippingRequest request)) {
            throw new IllegalArgumentException("Shipping requests can only be updated from shipping requests");
        }
        if (request.getItemCode() != null) {
            setItemCode(request.getItemCode());
        }
        if (request.getDestination() != null) {
            setDestination(request.getDestination());
        }
        if (request.getQuantity() != null) {
            setQuantity(request.getQuantity());
        }
        if (request.getStatus() != null) {
            setStatus(request.getStatus());
        }
        if (request.getRequestedDate() != null) {
            setRequestedDate(request.getRequestedDate());
        }
    }
}
