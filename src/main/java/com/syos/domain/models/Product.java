package com.syos.domain.models;

import com.syos.domain.valueobjects.ProductCode;
import com.syos.domain.valueobjects.Money;
import com.syos.domain.enums.UnitOfMeasure;

public class Product {
    private final ProductCode productCode;
    private final String productName;
    private final int categoryId;
    private final int subcategoryId;
    private final int brandId;
    private final Money unitPrice;
    private final String description;
    private final UnitOfMeasure unitOfMeasure;
    private final boolean isActive;

    public Product(ProductCode productCode, String productName, int categoryId,
                   int subcategoryId, int brandId, Money unitPrice, String description,
                   UnitOfMeasure unitOfMeasure, boolean isActive) {
        this.productCode = productCode;
        this.productName = productName;
        this.categoryId = categoryId;
        this.subcategoryId = subcategoryId;
        this.brandId = brandId;
        this.unitPrice = unitPrice;
        this.description = description;
        this.unitOfMeasure = unitOfMeasure;
        this.isActive = isActive;
    }

    // Getters
    public ProductCode getProductCode() { return productCode; }
    public String getProductName() { return productName; }
    public int getCategoryId() { return categoryId; }
    public int getSubcategoryId() { return subcategoryId; }
    public int getBrandId() { return brandId; }
    public Money getUnitPrice() { return unitPrice; }
    public String getDescription() { return description; }
    public UnitOfMeasure getUnitOfMeasure() { return unitOfMeasure; }
    public boolean isActive() { return isActive; }

    @Override
    public String toString() {
        return String.format("%s - %s (%s)",
                productCode, productName, unitPrice);
    }
}