package com.ifg.warehouse.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Full replacement of an item's own fields. Variants are managed through their own endpoints.
 */
public record UpdateItemRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 1000) String description,
        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal basePrice
) {
}
