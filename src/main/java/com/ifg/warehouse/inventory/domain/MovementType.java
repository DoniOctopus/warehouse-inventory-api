package com.ifg.warehouse.inventory.domain;

public enum MovementType {
    /** Stock a variant was created with. */
    INITIAL,
    /** Goods received from a supplier. */
    RESTOCK,
    /** Manual correction: stock count, damage, loss, etc. Can be positive or negative. */
    ADJUSTMENT,
    /** Units sold through an order. */
    SALE,
    /** Units returned to stock because an order was cancelled. */
    SALE_CANCELLED
}
