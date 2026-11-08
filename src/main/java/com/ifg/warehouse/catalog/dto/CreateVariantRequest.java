package com.ifg.warehouse.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;

/**
 * @param price        optional; when omitted the variant inherits the item's base price
 * @param initialStock optional; defaults to 0. Later changes go through the inventory endpoints.
 * @param attributes   free-form descriptors such as {"size": "M", "color": "Black"}
 */
public record CreateVariantRequest(
        @NotBlank @Size(max = 64)
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "may only contain letters, digits, '-' and '_'")
        String sku,
        @NotBlank @Size(max = 150) String name,
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal price,
        @Min(0) @Max(1_000_000) Integer initialStock,
        @Size(max = 10) Map<@NotBlank @Size(max = 50) String, @NotBlank @Size(max = 100) String> attributes
) {

    public int initialStockOrZero() {
        return initialStock == null ? 0 : initialStock;
    }
}
