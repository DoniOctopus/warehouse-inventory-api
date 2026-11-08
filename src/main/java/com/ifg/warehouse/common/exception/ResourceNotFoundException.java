package com.ifg.warehouse.common.exception;

/**
 * Thrown when a requested resource does not exist. Mapped to HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Object id) {
        super("%s with id %s was not found".formatted(resource, id));
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
