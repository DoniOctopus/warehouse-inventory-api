package com.ifg.warehouse.inventory.repository;

import com.ifg.warehouse.inventory.domain.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    Page<StockMovement> findByVariantId(Long variantId, Pageable pageable);
}
