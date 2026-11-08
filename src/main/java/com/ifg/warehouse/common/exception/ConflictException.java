package com.ifg.warehouse.common.exception;

/**
 * Thrown when a request conflicts with the current state of a resource,
 * for example a duplicate SKU. Mapped to HTTP 409.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
