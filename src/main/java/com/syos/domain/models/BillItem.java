package com.syos.domain.models;

import com.syos.domain.valueobjects.ProductCode;
import com.syos.domain.valueobjects.Money;

public class BillItem {
    private final ProductCode productCode;
    private final String productName;
    private final int quantity;
    private final Money unitPrice;
    private final Money totalPrice;
    private final int batchNumber; // main_inventory_id

    public BillItem(ProductCode productCode, String productName, int quantity,
                    Money unitPrice, int batchNumber) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        this.productCode = productCode;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalPrice = unitPrice.multiply(quantity);
        this.batchNumber = batchNumber;
    }

    // Getters
    public ProductCode getProductCode() { return productCode; }
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public Money getUnitPrice() { return unitPrice; }
    public Money getTotalPrice() { return totalPrice; }
    public int getBatchNumber() { return batchNumber; }

    @Override
    public String toString() {
        return String.format("%s x%d = %s",
                productName, quantity, totalPrice);
    }
}
