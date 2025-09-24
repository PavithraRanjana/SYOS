package com.syos.exceptions;

/**
 * Exception thrown when business rules are violated.
 * Distinguishes business rule violations from technical validation errors.
 *
 * Examples of business rules:
 * - Amounts must be positive
 * - Dates cannot be in the future
 * - Quantities must be greater than zero
 * - String lengths must be within business limits
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }

    public BusinessRuleException(String message, Throwable cause) {
        super(message, cause);
    }
}