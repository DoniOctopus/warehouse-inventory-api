package com.ifg.warehouse.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PlaceOrderRequest(
        @NotEmpty @Size(max = 50) List<@Valid @NotNull Line> items,
        @Size(max = 500) String note
) {

    public record Line(
            @NotNull Long variantId,
            @NotNull @Min(1) @Max(10_000) Integer quantity
    ) {
    }
}
