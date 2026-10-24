package com.infosys.commerce.domain.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.infosys.commerce.domain.enums.OrderStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_user", columnList = "userId"),
    @Index(name = "idx_order_status", columnList = "status"),
    @Index(name = "idx_order_idempotency", columnList = "idempotencyKey", unique = true)
})
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String customerEmail;

    @Column(nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Column(nullable = false, length = 500)
    private String shippingAddress;

    @Column(length = 500)
    private String failureReason;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public Order() {}

    public Order(Long userId, String customerEmail, String idempotencyKey, String shippingAddress, String currency) {
        this.userId = userId;
        this.customerEmail = customerEmail;
        this.idempotencyKey = idempotencyKey;
        this.shippingAddress = shippingAddress;
        this.currency = currency != null ? currency : "USD";
        this.status = OrderStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
        this.totalAmount = this.totalAmount.add(item.getSubtotal());
    }

    public void transitionTo(OrderStatus newStatus, String reason) {
        // Enforce valid state machine transitions
        boolean valid = switch (this.status) {
            case PENDING -> newStatus == OrderStatus.INVENTORY_RESERVED || newStatus == OrderStatus.FAILED || newStatus == OrderStatus.CANCELLED;
            case INVENTORY_RESERVED -> newStatus == OrderStatus.PAYMENT_AUTHORIZED || newStatus == OrderStatus.FAILED || newStatus == OrderStatus.CANCELLED;
            case PAYMENT_AUTHORIZED -> newStatus == OrderStatus.COMPLETED || newStatus == OrderStatus.CANCELLED;
            case COMPLETED, CANCELLED, FAILED -> false; // Terminal states
        };

        if (!valid) {
            throw new IllegalStateException("Illegal order transition from " + this.status + " to " + newStatus);
        }

        this.status = newStatus;
        if (reason != null) {
            this.failureReason = reason;
        }
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
