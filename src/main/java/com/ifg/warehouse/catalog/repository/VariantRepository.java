package com.ifg.warehouse.catalog.repository;

import com.ifg.warehouse.catalog.domain.Variant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VariantRepository extends JpaRepository<Variant, Long> {

    Optional<Variant> findByIdAndItemId(Long id, Long itemId);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    /**
     * Loads a variant with a database write lock ({@code SELECT ... FOR UPDATE}).
     * Concurrent transactions touching the same variant wait until this one commits,
     * so a stock check followed by a stock update cannot be interleaved.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Variant v where v.id = :id")
    Optional<Variant> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select v from Variant v join fetch v.item
            where v.stockQuantity <= :threshold
            order by v.stockQuantity asc, v.id asc
            """)
    List<Variant> findLowStock(@Param("threshold") int threshold);
}
