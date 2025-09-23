package com.syos.service.impl;

import com.syos.domain.models.Product;
import com.syos.domain.valueobjects.ProductCode;
import com.syos.domain.valueobjects.Money;
import com.syos.domain.enums.UnitOfMeasure;
import com.syos.repository.interfaces.ProductRepository;
import com.syos.service.interfaces.ProductCodeGenerator;

/**
 * Factory Pattern implementation for creating Product objects.
 * Handles the complex logic of product creation including:
 * - Product code generation
 * - Validation of product data
 * - Setting appropriate defaults
 */
public class ProductFactory {

    private final ProductCodeGenerator codeGenerator;
    private final ProductRepository productRepository;

    public ProductFactory(ProductCodeGenerator codeGenerator, ProductRepository productRepository) {
        this.codeGenerator = codeGenerator;
        this.productRepository = productRepository;
    }

    /**
     * Creates a new Product with auto-generated product code.
     *
     * @param productName Product name
     * @param categoryId Category ID
     * @param subcategoryId Subcategory ID
     * @param brandId Brand ID
     * @param unitPrice Unit price
     * @param description Product description
     * @param unitOfMeasure Unit of measure
     * @return Created Product
     */
    public Product createProduct(String productName,
                                 int categoryId,
                                 int subcategoryId,
                                 int brandId,
                                 Money unitPrice,
                                 String description,
                                 UnitOfMeasure unitOfMeasure) {

        // Validate inputs
        validateProductData(productName, categoryId, subcategoryId, brandId, unitPrice);

        // Generate unique product code
        ProductCode productCode = codeGenerator.generateProductCode(categoryId, subcategoryId, brandId);

        // Ensure code is unique
        int attempts = 0;
        while (productRepository.existsById(productCode) && attempts < 100) {
            productCode = codeGenerator.generateProductCode(categoryId, subcategoryId, brandId);
            attempts++;
        }

        if (productRepository.existsById(productCode)) {
            throw new IllegalStateException("Unable to generate unique product code after 100 attempts");
        }

        // Set defaults
        String finalDescription = description != null && !description.trim().isEmpty()
                ? description.trim()
                : "No description available";

        UnitOfMeasure finalUnitOfMeasure = unitOfMeasure != null
                ? unitOfMeasure
                : UnitOfMeasure.PCS;

        // Create product
        return new Product(
                productCode,
                productName.trim(),
                categoryId,
                subcategoryId,
                brandId,
                unitPrice,
                finalDescription,
                finalUnitOfMeasure,
                true // New products are active by default
        );
    }

    /**
     * Creates a product with a specific product code.
     * Used for testing or when code is predetermined.
     *
     * @param productCode Specific product code to use
     * @param productName Product name
     * @param categoryId Category ID
     * @param subcategoryId Subcategory ID
     * @param brandId Brand ID
     * @param unitPrice Unit price
     * @param description Product description
     * @param unitOfMeasure Unit of measure
     * @return Created Product
     */
    public Product createProductWithCode(ProductCode productCode,
                                         String productName,
                                         int categoryId,
                                         int subcategoryId,
                                         int brandId,
                                         Money unitPrice,
                                         String description,
                                         UnitOfMeasure unitOfMeasure) {

        // Validate inputs
        validateProductData(productName, categoryId, subcategoryId, brandId, unitPrice);

        if (productRepository.existsById(productCode)) {
            throw new IllegalArgumentException("Product with code " + productCode + " already exists");
        }

        // Set defaults
        String finalDescription = description != null && !description.trim().isEmpty()
                ? description.trim()
                : "No description available";

        UnitOfMeasure finalUnitOfMeasure = unitOfMeasure != null
                ? unitOfMeasure
                : UnitOfMeasure.PCS;

        return new Product(
                productCode,
                productName.trim(),
                categoryId,
                subcategoryId,
                brandId,
                unitPrice,
                finalDescription,
                finalUnitOfMeasure,
                true
        );
    }

    /**
     * Creates a copy of an existing product with modifications.
     * Useful for creating product variants.
     *
     * @param baseProduct Base product to copy from
     * @param modifications Modifications to apply
     * @return New product with modifications
     */
    public Product createProductVariant(Product baseProduct, ProductModifications modifications) {
        return createProduct(
                modifications.getProductName() != null ? modifications.getProductName() : baseProduct.getProductName(),
                modifications.getCategoryId() != null ? modifications.getCategoryId() : baseProduct.getCategoryId(),
                modifications.getSubcategoryId() != null ? modifications.getSubcategoryId() : baseProduct.getSubcategoryId(),
                modifications.getBrandId() != null ? modifications.getBrandId() : baseProduct.getBrandId(),
                modifications.getUnitPrice() != null ? modifications.getUnitPrice() : baseProduct.getUnitPrice(),
                modifications.getDescription() != null ? modifications.getDescription() : baseProduct.getDescription(),
                modifications.getUnitOfMeasure() != null ? modifications.getUnitOfMeasure() : baseProduct.getUnitOfMeasure()
        );
    }

    private void validateProductData(String productName, int categoryId, int subcategoryId,
                                     int brandId, Money unitPrice) {

        if (productName == null || productName.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name is required");
        }

        if (productName.trim().length() > 200) {
            throw new IllegalArgumentException("Product name cannot exceed 200 characters");
        }

        if (categoryId <= 0) {
            throw new IllegalArgumentException("Category ID must be positive");
        }

        if (subcategoryId <= 0) {
            throw new IllegalArgumentException("Subcategory ID must be positive");
        }

        if (brandId <= 0) {
            throw new IllegalArgumentException("Brand ID must be positive");
        }

        if (unitPrice == null) {
            throw new IllegalArgumentException("Unit price is required");
        }

        if (unitPrice.getAmount().doubleValue() <= 0) {
            throw new IllegalArgumentException("Unit price must be positive");
        }
    }

    /**
     * Helper class for product modifications.
     */
    public static class ProductModifications {
        private String productName;
        private Integer categoryId;
        private Integer subcategoryId;
        private Integer brandId;
        private Money unitPrice;
        private String description;
        private UnitOfMeasure unitOfMeasure;

        public ProductModifications setProductName(String productName) {
            this.productName = productName;
            return this;
        }

        public ProductModifications setCategoryId(Integer categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        public ProductModifications setSubcategoryId(Integer subcategoryId) {
            this.subcategoryId = subcategoryId;
            return this;
        }

        public ProductModifications setBrandId(Integer brandId) {
            this.brandId = brandId;
            return this;
        }

        public ProductModifications setUnitPrice(Money unitPrice) {
            this.unitPrice = unitPrice;
            return this;
        }

        public ProductModifications setDescription(String description) {
            this.description = description;
            return this;
        }

        public ProductModifications setUnitOfMeasure(UnitOfMeasure unitOfMeasure) {
            this.unitOfMeasure = unitOfMeasure;
            return this;
        }

        // Getters
        public String getProductName() { return productName; }
        public Integer getCategoryId() { return categoryId; }
        public Integer getSubcategoryId() { return subcategoryId; }
        public Integer getBrandId() { return brandId; }
        public Money getUnitPrice() { return unitPrice; }
        public String getDescription() { return description; }
        public UnitOfMeasure getUnitOfMeasure() { return unitOfMeasure; }
    }
}