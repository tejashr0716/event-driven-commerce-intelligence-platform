package com.infosys.commerce.domain.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "inventory")
public class Inventory {

    @Id
    @Column(nullable = false, length = 64)
    private String sku;

    @Column(nullable = false)
    private int totalStock;

    @Column(nullable = false)
    private int reservedStock = 0;

    @Column(nullable = false)
    private int availableStock;

    @Version
    private Long version;

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public Inventory() {}

    public Inventory(String sku, int totalStock) {
        this.sku = sku;
        this.totalStock = totalStock;
        this.reservedStock = 0;
        this.availableStock = totalStock;
        this.updatedAt = Instant.now();
    }

    public void reserve(int quantity) {
        if (this.availableStock < quantity) {
            throw new IllegalStateException("Insufficient stock for SKU " + sku + ". Available: " + availableStock + ", requested: " + quantity);
        }
        this.reservedStock += quantity;
        this.availableStock -= quantity;
        this.updatedAt = Instant.now();
    }

    public void release(int quantity) {
        int toRelease = Math.min(this.reservedStock, quantity);
        this.reservedStock -= toRelease;
        this.availableStock += toRelease;
        this.updatedAt = Instant.now();
    }

    public void commitReservation(int quantity) {
        int toCommit = Math.min(this.reservedStock, quantity);
        this.reservedStock -= toCommit;
        this.totalStock -= toCommit;
        this.updatedAt = Instant.now();
    }

    public void restock(int additionalQuantity) {
        if (additionalQuantity > 0) {
            this.totalStock += additionalQuantity;
            this.availableStock += additionalQuantity;
            this.updatedAt = Instant.now();
        }
    }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public int getTotalStock() { return totalStock; }
    public void setTotalStock(int totalStock) { this.totalStock = totalStock; }

    public int getReservedStock() { return reservedStock; }
    public void setReservedStock(int reservedStock) { this.reservedStock = reservedStock; }

    public int getAvailableStock() { return availableStock; }
    public void setAvailableStock(int availableStock) { this.availableStock = availableStock; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
