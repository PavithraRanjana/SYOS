package com.syos.exceptions;

/**
 * Custom exception for inventory-related operations.
 * Extends RuntimeException to avoid forced exception handling in simple cases.
 */
public class InventoryException extends RuntimeException {

    public InventoryException(String message) {
        super(message);
    }

    public InventoryException(String message, Throwable cause) {
        super(message, cause);
    }

    public InventoryException(Throwable cause) {
        super(cause);
    }
}