package com.ifg.warehouse.catalog.dto;

import com.ifg.warehouse.catalog.domain.Item;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * @param totalStock units on hand summed over all variants
 */
public record ItemResponse(
        Long id,
        String name,
        String description,
        BigDecimal basePrice,
        int totalStock,
        boolean inStock,
        List<VariantResponse> variants,
        Instant createdAt,
        Instant updatedAt
) {

    public static ItemResponse from(Item item) {
        int totalStock = item.getTotalStock();
        return new ItemResponse(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getBasePrice(),
                totalStock,
                totalStock > 0,
                item.getVariants().stream().map(VariantResponse::from).toList(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
