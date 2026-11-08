package com.ifg.warehouse.catalog.domain;

import com.ifg.warehouse.common.exception.InsufficientStockException;
import com.ifg.warehouse.common.persistence.AuditableEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A concrete, sellable version of an {@link Item}: the stock-keeping unit (SKU).
 * <p>
 * Price is optional: when it is not set the variant inherits the item's base price.
 * Stock is tracked here, because "how many black T-shirts in size M do we have" is
 * the question the warehouse actually needs to answer.
 */
@Entity
@Table(name = "variants")
public class Variant extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false, unique = true, length = 64)
    private String sku;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @ElementCollection
    @CollectionTable(name = "variant_attributes", joinColumns = @JoinColumn(name = "variant_id"))
    @MapKeyColumn(name = "attr_name", length = 50)
    @Column(name = "attr_value", nullable = false, length = 100)
    private Map<String, String> attributes = new LinkedHashMap<>();

    /** Optimistic locking: protects against lost updates when two clients edit the same variant. */
    @Version
    private long version;

    protected Variant() {
        // for JPA
    }

    public Variant(String sku, String name, BigDecimal price, int initialStock, Map<String, String> attributes) {
        if (initialStock < 0) {
            throw new IllegalArgumentException("Initial stock cannot be negative");
        }
        this.sku = sku;
        this.name = name;
        this.price = price;
        this.stockQuantity = initialStock;
        replaceAttributes(attributes);
    }

    void assignTo(Item item) {
        this.item = item;
    }

    public void updateDetails(String sku, String name, BigDecimal price, Map<String, String> attributes) {
        this.sku = sku;
        this.name = name;
        this.price = price;
        replaceAttributes(attributes);
    }

    private void replaceAttributes(Map<String, String> newAttributes) {
        this.attributes.clear();
        if (newAttributes != null) {
            this.attributes.putAll(newAttributes);
        }
    }

    /**
     * Adds units to stock (restock, returned goods, cancelled order).
     */
    public void increaseStock(int quantity) {
        requirePositive(quantity);
        stockQuantity = Math.addExact(stockQuantity, quantity);
    }

    /**
     * Removes units from stock. Refuses to go below zero, which is what prevents
     * selling items that are out of stock. Callers must hold a write lock on the row
     * (see {@code VariantRepository#findByIdForUpdate}) so the check-then-update is atomic.
     *
     * @throws InsufficientStockException if fewer than {@code quantity} units are on hand
     */
    public void decreaseStock(int quantity) {
        requirePositive(quantity);
        if (quantity > stockQuantity) {
            throw new InsufficientStockException(sku, quantity, stockQuantity);
        }
        stockQuantity -= quantity;
    }

    private static void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive, was " + quantity);
        }
    }

    /** The price a customer pays: the variant's own price, or the item's base price if none is set. */
    public BigDecimal getEffectivePrice() {
        return Objects.requireNonNullElse(price, item.getBasePrice());
    }

    public boolean isInStock() {
        return stockQuantity > 0;
    }

    public Long getId() {
        return id;
    }

    public Item getItem() {
        return item;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public long getVersion() {
        return version;
    }
}
