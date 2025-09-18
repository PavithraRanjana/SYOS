package com.syos.domain.models;

import com.syos.domain.valueobjects.ProductCode;
import com.syos.domain.valueobjects.Money;
import java.time.LocalDate;

public class MainInventory {
    private final int batchNumber; // main_inventory_id
    private final ProductCode productCode;
    private final int quantityReceived;
    private final Money purchasePrice;
    private final LocalDate purchaseDate;
    private final LocalDate expiryDate;
    private final String supplierName;
    private int remainingQuantity;

    public MainInventory(int batchNumber, ProductCode productCode, int quantityReceived,
                         Money purchasePrice, LocalDate purchaseDate, LocalDate expiryDate,
                         String supplierName, int remainingQuantity) {
        this.batchNumber = batchNumber;
        this.productCode = productCode;
        this.quantityReceived = quantityReceived;
        this.purchasePrice = purchasePrice;
        this.purchaseDate = purchaseDate;
        this.expiryDate = expiryDate;
        this.supplierName = supplierName;
        this.remainingQuantity = remainingQuantity;
    }

    public boolean hasEnoughStock(int requiredQuantity) {
        return remainingQuantity >= requiredQuantity;
    }

    public void reduceStock(int quantity) {
        if (quantity > remainingQuantity) {
            throw new IllegalArgumentException("Insufficient stock in batch");
        }
        this.remainingQuantity -= quantity;
    }

    // Getters
    public int getBatchNumber() { return batchNumber; }
    public ProductCode getProductCode() { return productCode; }
    public int getQuantityReceived() { return quantityReceived; }
    public Money getPurchasePrice() { return purchasePrice; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public String getSupplierName() { return supplierName; }
    public int getRemainingQuantity() { return remainingQuantity; }
}
