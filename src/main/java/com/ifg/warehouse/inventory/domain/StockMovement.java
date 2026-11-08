package com.ifg.warehouse.inventory.domain;

import com.ifg.warehouse.catalog.domain.Variant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * One immutable entry in the stock ledger of a variant.
 */
@Entity
@Table(name = "stock_movements")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Plain id rather than an association: the ledger is append-only and never needs to
     * navigate to the variant. Rows are removed by the database (ON DELETE CASCADE)
     * when their variant is deleted.
     */
    @Column(name = "variant_id", nullable = false, updatable = false)
    private Long variantId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20, updatable = false)
    private MovementType type;

    @Column(name = "quantity_change", nullable = false, updatable = false)
    private int quantityChange;

    @Column(name = "quantity_after", nullable = false, updatable = false)
    private int quantityAfter;

    @Column(length = 255, updatable = false)
    private String reason;

    @Column(length = 64, updatable = false)
    private String reference;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected StockMovement() {
        // for JPA
    }

    public StockMovement(Variant variant, MovementType type, int quantityChange, String reason, String reference) {
        this.variantId = variant.getId();
        this.type = type;
        this.quantityChange = quantityChange;
        this.quantityAfter = variant.getStockQuantity();
        this.reason = reason;
        this.reference = reference;
    }

    public Long getId() {
        return id;
    }

    public Long getVariantId() {
        return variantId;
    }

    public MovementType getType() {
        return type;
    }

    public int getQuantityChange() {
        return quantityChange;
    }

    public int getQuantityAfter() {
        return quantityAfter;
    }

    public String getReason() {
        return reason;
    }

    public String getReference() {
        return reference;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
