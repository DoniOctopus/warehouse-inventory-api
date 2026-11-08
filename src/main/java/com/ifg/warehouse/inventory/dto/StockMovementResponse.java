package com.ifg.warehouse.inventory.dto;

import com.ifg.warehouse.inventory.domain.MovementType;
import com.ifg.warehouse.inventory.domain.StockMovement;

import java.time.Instant;

public record StockMovementResponse(
        Long id,
        Long variantId,
        MovementType type,
        int quantityChange,
        int quantityAfter,
        String reason,
        String reference,
        Instant createdAt
) {

    public static StockMovementResponse from(StockMovement movement) {
        return new StockMovementResponse(
                movement.getId(),
                movement.getVariantId(),
                movement.getType(),
                movement.getQuantityChange(),
                movement.getQuantityAfter(),
                movement.getReason(),
                movement.getReference(),
                movement.getCreatedAt()
        );
    }
}
