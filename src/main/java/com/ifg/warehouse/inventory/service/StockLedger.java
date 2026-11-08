package com.ifg.warehouse.inventory.service;

import com.ifg.warehouse.catalog.domain.Variant;
import com.ifg.warehouse.inventory.domain.MovementType;
import com.ifg.warehouse.inventory.domain.StockMovement;
import com.ifg.warehouse.inventory.repository.StockMovementRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The single place where stock levels change. Every change updates the variant
 * and appends a {@link StockMovement} in the same transaction, so the ledger and
 * the current stock can never disagree.
 * <p>
 * Methods must run inside the caller's transaction, on a variant the caller has
 * locked for update.
 */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class StockLedger {

    private final StockMovementRepository movementRepository;

    public StockLedger(StockMovementRepository movementRepository) {
        this.movementRepository = movementRepository;
    }

    /**
     * Applies a signed change to the variant's stock and records it.
     *
     * @throws com.ifg.warehouse.common.exception.InsufficientStockException if stock would go negative
     */
    public StockMovement apply(Variant variant, int quantityChange, MovementType type,
                               String reason, String reference) {
        if (quantityChange > 0) {
            variant.increaseStock(quantityChange);
        } else if (quantityChange < 0) {
            variant.decreaseStock(-quantityChange);
        } else {
            throw new IllegalArgumentException("Quantity change must not be zero");
        }
        return movementRepository.save(new StockMovement(variant, type, quantityChange, reason, reference));
    }

    /**
     * Records the stock a freshly created variant starts with. The variant already
     * holds that stock, so only the ledger entry is written.
     */
    public void recordInitialStock(Variant variant) {
        if (variant.getStockQuantity() > 0) {
            movementRepository.save(new StockMovement(
                    variant, MovementType.INITIAL, variant.getStockQuantity(), "Initial stock", null));
        }
    }
}
