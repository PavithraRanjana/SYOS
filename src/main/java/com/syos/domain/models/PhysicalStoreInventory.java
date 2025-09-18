package com.syos.domain.models;

import com.syos.domain.valueobjects.ProductCode;
import java.time.LocalDate;

public class PhysicalStoreInventory {
    private final int physicalInventoryId;
    private final ProductCode productCode;
    private final int batchNumber; // main_inventory_id
    private int quantityOnShelf;
    private final LocalDate restockedDate;

    public PhysicalStoreInventory(int physicalInventoryId, ProductCode productCode,
                                  int batchNumber, int quantityOnShelf, LocalDate restockedDate) {
        this.physicalInventoryId = physicalInventoryId;
        this.productCode = productCode;
        this.batchNumber = batchNumber;
        this.quantityOnShelf = quantityOnShelf;
        this.restockedDate = restockedDate;
    }

    public boolean hasEnoughStock(int requiredQuantity) {
        return quantityOnShelf >= requiredQuantity;
    }

    public void reduceStock(int quantity) {
        if (quantity > quantityOnShelf) {
            throw new IllegalArgumentException("Insufficient stock");
        }
        this.quantityOnShelf -= quantity;
    }

    // Getters
    public int getPhysicalInventoryId() { return physicalInventoryId; }
    public ProductCode getProductCode() { return productCode; }
    public int getBatchNumber() { return batchNumber; }
    public int getQuantityOnShelf() { return quantityOnShelf; }
    public LocalDate getRestockedDate() { return restockedDate; }
}