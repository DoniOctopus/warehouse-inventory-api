package com.ifg.warehouse.catalog.dto;

import com.ifg.warehouse.catalog.domain.Variant;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * @param price          the variant's own price, or {@code null} when it inherits the item price
 * @param effectivePrice the price actually charged when this variant is sold
 */
public record VariantResponse(
        Long id,
        Long itemId,
        String sku,
        String name,
        Map<String, String> attributes,
        BigDecimal price,
        BigDecimal effectivePrice,
        int stockQuantity,
        boolean inStock,
        Instant createdAt,
        Instant updatedAt
) {

    public static VariantResponse from(Variant variant) {
        return new VariantResponse(
                variant.getId(),
                variant.getItem().getId(),
                variant.getSku(),
                variant.getName(),
                Map.copyOf(variant.getAttributes()),
                variant.getPrice(),
                variant.getEffectivePrice(),
                variant.getStockQuantity(),
                variant.isInStock(),
                variant.getCreatedAt(),
                variant.getUpdatedAt()
        );
    }
}
