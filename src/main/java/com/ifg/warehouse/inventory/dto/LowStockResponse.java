package com.ifg.warehouse.inventory.dto;

import com.ifg.warehouse.catalog.domain.Variant;

public record LowStockResponse(
        Long itemId,
        String itemName,
        Long variantId,
        String sku,
        String variantName,
        int stockQuantity
) {

    public static LowStockResponse from(Variant variant) {
        return new LowStockResponse(
                variant.getItem().getId(),
                variant.getItem().getName(),
                variant.getId(),
                variant.getSku(),
                variant.getName(),
                variant.getStockQuantity()
        );
    }
}
