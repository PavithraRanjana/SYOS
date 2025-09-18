package com.syos.exceptions;

public class InsufficientStockException extends RuntimeException {
    private final int availableStock;
    private final int requestedQuantity;

    public InsufficientStockException(String message, int availableStock, int requestedQuantity) {
        super(message);
        this.availableStock = availableStock;
        this.requestedQuantity = requestedQuantity;
    }

    public int getAvailableStock() { return availableStock; }
    public int getRequestedQuantity() { return requestedQuantity; }
}