package com.ifg.warehouse.order.domain;

public enum OrderStatus {
    /** Stock has been deducted; the sale is final unless cancelled. */
    COMPLETED,
    /** The order was cancelled and its stock returned. */
    CANCELLED
}
