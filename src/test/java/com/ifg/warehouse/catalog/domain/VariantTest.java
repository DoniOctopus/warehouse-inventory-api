package com.ifg.warehouse.catalog.domain;

import com.ifg.warehouse.common.exception.InsufficientStockException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VariantTest {

    private Variant variantWithStock(int stock, BigDecimal ownPrice) {
        Item item = new Item("Shirt", null, new BigDecimal("100.00"));
        Variant variant = new Variant("SKU-1", "Default", ownPrice, stock, Map.of());
        item.addVariant(variant);
        return variant;
    }

    @Test
    void decreasesStockWhenEnoughUnitsAreOnHand() {
        Variant variant = variantWithStock(5, null);

        variant.decreaseStock(5);

        assertThat(variant.getStockQuantity()).isZero();
        assertThat(variant.isInStock()).isFalse();
    }

    @Test
    void refusesToSellMoreThanIsInStock() {
        Variant variant = variantWithStock(2, null);

        assertThatThrownBy(() -> variant.decreaseStock(3))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("requested 3, available 2");
        assertThat(variant.getStockQuantity()).isEqualTo(2);
    }

    @Test
    void increasesStock() {
        Variant variant = variantWithStock(0, null);

        variant.increaseStock(7);

        assertThat(variant.getStockQuantity()).isEqualTo(7);
    }

    @Test
    void rejectsNonPositiveQuantities() {
        Variant variant = variantWithStock(1, null);

        assertThatIllegalArgumentException().isThrownBy(() -> variant.decreaseStock(0));
        assertThatIllegalArgumentException().isThrownBy(() -> variant.increaseStock(-1));
    }

    @Test
    void effectivePriceFallsBackToItemBasePrice() {
        assertThat(variantWithStock(1, null).getEffectivePrice()).isEqualByComparingTo("100.00");
        assertThat(variantWithStock(1, new BigDecimal("120.00")).getEffectivePrice()).isEqualByComparingTo("120.00");
    }
}
