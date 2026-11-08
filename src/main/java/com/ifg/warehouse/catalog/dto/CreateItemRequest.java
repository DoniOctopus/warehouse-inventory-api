package com.ifg.warehouse.catalog.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Creates an item, optionally together with its variants in a single request.
 */
public record CreateItemRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 1000) String description,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal basePrice,
        @Valid @Size(max = 100) List<CreateVariantRequest> variants
) {

    public List<CreateVariantRequest> variantsOrEmpty() {
        return variants == null ? List.of() : variants;
    }
}
