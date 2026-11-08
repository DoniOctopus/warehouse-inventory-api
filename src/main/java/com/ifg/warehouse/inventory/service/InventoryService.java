package com.ifg.warehouse.inventory.service;

import com.ifg.warehouse.catalog.domain.Variant;
import com.ifg.warehouse.catalog.repository.VariantRepository;
import com.ifg.warehouse.common.exception.BusinessRuleException;
import com.ifg.warehouse.common.exception.ResourceNotFoundException;
import com.ifg.warehouse.common.web.PageResponse;
import com.ifg.warehouse.inventory.domain.MovementType;
import com.ifg.warehouse.inventory.dto.LowStockResponse;
import com.ifg.warehouse.inventory.dto.StockMovementRequest;
import com.ifg.warehouse.inventory.dto.StockMovementResponse;
import com.ifg.warehouse.inventory.repository.StockMovementRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class InventoryService {

    private final VariantRepository variantRepository;
    private final StockMovementRepository movementRepository;
    private final StockLedger stockLedger;

    public InventoryService(VariantRepository variantRepository,
                            StockMovementRepository movementRepository,
                            StockLedger stockLedger) {
        this.variantRepository = variantRepository;
        this.movementRepository = movementRepository;
        this.stockLedger = stockLedger;
    }

    public StockMovementResponse recordMovement(Long variantId, StockMovementRequest request) {
        int quantity = request.quantity();
        if (quantity == 0) {
            throw new BusinessRuleException("Quantity must not be zero");
        }
        if (request.type() == StockMovementRequest.Type.RESTOCK && quantity < 0) {
            throw new BusinessRuleException("A restock must add stock; use an ADJUSTMENT to remove units");
        }

        Variant variant = variantRepository.findByIdForUpdate(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant", variantId));
        MovementType type = MovementType.valueOf(request.type().name());

        return StockMovementResponse.from(
                stockLedger.apply(variant, quantity, type, request.reason(), null));
    }

    @Transactional(readOnly = true)
    public PageResponse<StockMovementResponse> history(Long variantId, Pageable pageable) {
        if (!variantRepository.existsById(variantId)) {
            throw new ResourceNotFoundException("Variant", variantId);
        }
        return PageResponse.of(movementRepository.findByVariantId(variantId, pageable),
                StockMovementResponse::from);
    }

    @Transactional(readOnly = true)
    public List<LowStockResponse> lowStock(int threshold) {
        return variantRepository.findLowStock(threshold).stream()
                .map(LowStockResponse::from)
                .toList();
    }
}
