package com.shopsphere.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_audit_log", indexes = {
    @Index(name = "idx_inventory_audit_product", columnList = "product_id")
})
public class InventoryAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "stock_before")
    private Integer stockBefore;

    @Column(name = "quantity_changed", nullable = false)
    private Integer quantityChanged;

    @Column(name = "stock_after", nullable = false)
    private Integer stockAfter;

    @Column(name = "movement_type", nullable = false, length = 50)
    private String movementType;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public InventoryAuditLog() {
    }

    public InventoryAuditLog(Product product, Integer quantityChanged, Integer stockAfter,
                             String movementType, String referenceId, String reason) {
        this.product = product;
        this.stockBefore = stockAfter != null && quantityChanged != null ? stockAfter - quantityChanged : null;
        this.quantityChanged = quantityChanged;
        this.stockAfter = stockAfter;
        this.movementType = movementType;
        this.referenceId = referenceId;
        this.reason = reason;
    }

    public InventoryAuditLog(Product product, Integer stockBefore, Integer quantityChanged, Integer stockAfter,
                             String movementType, String referenceId, String reason, User user) {
        this.product = product;
        this.stockBefore = stockBefore;
        this.quantityChanged = quantityChanged;
        this.stockAfter = stockAfter;
        this.movementType = movementType;
        this.referenceId = referenceId;
        this.reason = reason;
        this.user = user;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Integer getQuantityChanged() {
        return quantityChanged;
    }

    public void setQuantityChanged(Integer quantityChanged) {
        this.quantityChanged = quantityChanged;
    }

    public Integer getStockBefore() {
        return stockBefore;
    }

    public void setStockBefore(Integer stockBefore) {
        this.stockBefore = stockBefore;
    }

    public Integer getStockAfter() {
        return stockAfter;
    }

    public void setStockAfter(Integer stockAfter) {
        this.stockAfter = stockAfter;
    }

    public String getMovementType() {
        return movementType;
    }

    public void setMovementType(String movementType) {
        this.movementType = movementType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
