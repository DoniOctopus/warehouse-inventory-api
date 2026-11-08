package com.ifg.warehouse.common.exception;

/**
 * Thrown when a syntactically valid request violates a business rule.
 * Mapped to HTTP 422.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
