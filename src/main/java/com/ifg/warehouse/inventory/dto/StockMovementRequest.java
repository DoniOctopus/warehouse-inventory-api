package com.ifg.warehouse.inventory.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A manual stock change made by warehouse staff.
 *
 * @param quantity signed change: RESTOCK must be positive, ADJUSTMENT may be positive or negative
 */
public record StockMovementRequest(
        @NotNull Type type,
        @NotNull @Min(-1_000_000) @Max(1_000_000) Integer quantity,
        @Size(max = 255) String reason
) {

    /** The movement types that may be created manually; SALE and friends come from orders only. */
    public enum Type {
        RESTOCK,
        ADJUSTMENT
    }
}
