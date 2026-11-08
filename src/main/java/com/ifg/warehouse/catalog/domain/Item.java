package com.ifg.warehouse.catalog.domain;

import com.ifg.warehouse.common.persistence.AuditableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A product the shop sells, e.g. "Basic T-Shirt".
 * <p>
 * An item is a catalog entry; it is not sold directly. Each concrete, sellable
 * version of it (size M / colour black, ...) is a {@link Variant}, which carries its own stock.
 */
@Entity
@Table(name = "items")
public class Item extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<Variant> variants = new ArrayList<>();

    protected Item() {
        // for JPA
    }

    public Item(String name, String description, BigDecimal basePrice) {
        this.name = name;
        this.description = description;
        this.basePrice = basePrice;
    }

    public void updateDetails(String name, String description, BigDecimal basePrice) {
        this.name = name;
        this.description = description;
        this.basePrice = basePrice;
    }

    public void addVariant(Variant variant) {
        variants.add(variant);
        variant.assignTo(this);
    }

    public void removeVariant(Variant variant) {
        variants.remove(variant);
    }

    /** Total units on hand across all variants. */
    public int getTotalStock() {
        return variants.stream().mapToInt(Variant::getStockQuantity).sum();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public List<Variant> getVariants() {
        return Collections.unmodifiableList(variants);
    }
}
