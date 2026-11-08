package com.ifg.warehouse.catalog.repository;

import com.ifg.warehouse.catalog.domain.Variant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VariantRepository extends JpaRepository<Variant, Long> {

    Optional<Variant> findByIdAndItemId(Long id, Long itemId);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);
}
