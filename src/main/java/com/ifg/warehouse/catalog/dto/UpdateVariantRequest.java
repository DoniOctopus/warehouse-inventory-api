package com.ifg.warehouse.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Full replacement of a variant's descriptive fields.
 * <p>
 * Stock is intentionally not part of this request: stock only changes through
 * stock movements and orders, so every change is traceable.
 */
public record UpdateVariantRequest(
        @NotBlank @Size(max = 64)
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "may only contain letters, digits, '-' and '_'")
        String sku,
        @NotBlank @Size(max = 150) String name,
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal price,
        @Size(max = 10) Map<@NotBlank @Size(max = 50) String, @NotBlank @Size(max = 100) String> attributes
) {
}
