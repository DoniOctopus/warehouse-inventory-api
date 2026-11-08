package com.ifg.warehouse.catalog.service;

import com.ifg.warehouse.catalog.repository.VariantRepository;
import com.ifg.warehouse.common.exception.ConflictException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Normalises SKUs and enforces their global uniqueness.
 * SKUs are compared case-insensitively by storing them upper-case.
 */
@Component
public class SkuPolicy {

    private final VariantRepository variantRepository;

    public SkuPolicy(VariantRepository variantRepository) {
        this.variantRepository = variantRepository;
    }

    public static String normalize(String sku) {
        return sku.trim().toUpperCase(Locale.ROOT);
    }

    /** Ensures none of the given (already normalised) SKUs repeat or exist in the database. */
    public void assertAvailable(Collection<String> skus) {
        Set<String> seen = new HashSet<>();
        for (String sku : skus) {
            if (!seen.add(sku)) {
                throw new ConflictException("SKU '%s' appears more than once in the request".formatted(sku));
            }
            if (variantRepository.existsBySku(sku)) {
                throw new ConflictException("SKU '%s' is already in use".formatted(sku));
            }
        }
    }

    /** Ensures the SKU is not used by any variant other than {@code variantId}. */
    public void assertAvailableFor(String sku, Long variantId) {
        if (variantRepository.existsBySkuAndIdNot(sku, variantId)) {
            throw new ConflictException("SKU '%s' is already in use".formatted(sku));
        }
    }
}
