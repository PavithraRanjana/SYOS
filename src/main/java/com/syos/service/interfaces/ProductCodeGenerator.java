package com.syos.service.interfaces;

import com.syos.domain.valueobjects.ProductCode;

/**
 * Interface for generating product codes.
 * Part of the Factory Pattern implementation.
 */
public interface ProductCodeGenerator {

    /**
     * Generates a unique product code based on category, subcategory, and brand.
     *
     * @param categoryId Category ID
     * @param subcategoryId Subcategory ID
     * @param brandId Brand ID
     * @return Generated ProductCode
     */
    ProductCode generateProductCode(int categoryId, int subcategoryId, int brandId);

    /**
     * Generates the next sequence number for a given category/subcategory/brand combination.
     *
     * @param categoryId Category ID
     * @param subcategoryId Subcategory ID
     * @param brandId Brand ID
     * @return Next sequence number
     */
    int getNextSequenceNumber(int categoryId, int subcategoryId, int brandId);
}