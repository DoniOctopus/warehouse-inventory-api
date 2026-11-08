package com.ifg.warehouse.inventory.web;

import com.ifg.warehouse.common.web.PageResponse;
import com.ifg.warehouse.inventory.dto.LowStockResponse;
import com.ifg.warehouse.inventory.dto.StockMovementRequest;
import com.ifg.warehouse.inventory.dto.StockMovementResponse;
import com.ifg.warehouse.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Inventory", description = "Stock movements and stock levels")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/variants/{variantId}/stock-movements")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Restock or adjust the stock of a variant")
    public StockMovementResponse recordMovement(@PathVariable Long variantId,
                                                @Valid @RequestBody StockMovementRequest request) {
        return inventoryService.recordMovement(variantId, request);
    }

    @GetMapping("/variants/{variantId}/stock-movements")
    @Operation(summary = "Stock history of a variant, newest first (paginated)")
    public PageResponse<StockMovementResponse> history(
            @PathVariable Long variantId,
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable) {
        return inventoryService.history(variantId, pageable);
    }

    @GetMapping("/inventory/low-stock")
    @Operation(summary = "Variants whose stock is at or below a threshold")
    public List<LowStockResponse> lowStock(
            @RequestParam(defaultValue = "5") @Min(0) @Max(1_000_000) int threshold) {
        return inventoryService.lowStock(threshold);
    }
}
