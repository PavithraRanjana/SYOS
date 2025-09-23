package com.syos.service.impl;

import com.syos.domain.valueobjects.ProductCode;
import com.syos.service.interfaces.ProductCodeGenerator;
import com.syos.repository.interfaces.ProductRepository;
import com.syos.utils.DatabaseConnection;
import java.sql.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Implementation of ProductCodeGenerator that creates codes in format:
 * [CATEGORY_CODE][SUBCATEGORY_CODE][BRAND_CODE][SEQUENCE]
 * Example: BVEDRB001 (Beverages-EnergyDrink-RedBull-001)
 */
public class ProductCodeGeneratorImpl implements ProductCodeGenerator {

    private final Connection connection;
    private final Map<String, String> categoryCodeCache = new ConcurrentHashMap<>();
    private final Map<String, String> subcategoryCodeCache = new ConcurrentHashMap<>();
    private final Map<String, String> brandCodeCache = new ConcurrentHashMap<>();

    public ProductCodeGeneratorImpl() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    @Override
    public ProductCode generateProductCode(int categoryId, int subcategoryId, int brandId) {
        try {
            // Get codes for each component
            String categoryCode = getCategoryCode(categoryId);
            String subcategoryCode = getSubcategoryCode(subcategoryId);
            String brandCode = getBrandCode(brandId);

            // Get next sequence number
            int sequenceNumber = getNextSequenceNumber(categoryId, subcategoryId, brandId);

            // Create product code: CATEGORY + SUBCATEGORY + BRAND + SEQUENCE
            String codeString = String.format("%s%s%s%03d",
                    categoryCode, subcategoryCode, brandCode, sequenceNumber);

            return new ProductCode(codeString);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to generate product code", e);
        }
    }

    @Override
    public int getNextSequenceNumber(int categoryId, int subcategoryId, int brandId) {
        try {
            // Get codes for pattern matching
            String categoryCode = getCategoryCode(categoryId);
            String subcategoryCode = getSubcategoryCode(subcategoryId);
            String brandCode = getBrandCode(brandId);

            String basePattern = categoryCode + subcategoryCode + brandCode;

            String sql = """
                SELECT COALESCE(MAX(CAST(RIGHT(product_code, 3) AS UNSIGNED)), 0) + 1 as next_sequence
                FROM product 
                WHERE product_code LIKE ?
                """;

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, basePattern + "%");
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    return rs.getInt("next_sequence");
                }
            }

            return 1; // First product for this combination

        } catch (SQLException e) {
            throw new RuntimeException("Failed to get next sequence number", e);
        }
    }

    private String getCategoryCode(int categoryId) throws SQLException {
        String cacheKey = "cat_" + categoryId;
        return categoryCodeCache.computeIfAbsent(cacheKey, k -> {
            try {
                String sql = "SELECT category_code FROM category WHERE category_id = ?";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setInt(1, categoryId);
                    ResultSet rs = stmt.executeQuery();

                    if (rs.next()) {
                        return rs.getString("category_code");
                    } else {
                        throw new IllegalArgumentException("Category not found with ID: " + categoryId);
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to get category code", e);
            }
        });
    }

    private String getSubcategoryCode(int subcategoryId) throws SQLException {
        String cacheKey = "subcat_" + subcategoryId;
        return subcategoryCodeCache.computeIfAbsent(cacheKey, k -> {
            try {
                String sql = "SELECT subcategory_code FROM subcategory WHERE subcategory_id = ?";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setInt(1, subcategoryId);
                    ResultSet rs = stmt.executeQuery();

                    if (rs.next()) {
                        return rs.getString("subcategory_code");
                    } else {
                        throw new IllegalArgumentException("Subcategory not found with ID: " + subcategoryId);
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to get subcategory code", e);
            }
        });
    }

    private String getBrandCode(int brandId) throws SQLException {
        String cacheKey = "brand_" + brandId;
        return brandCodeCache.computeIfAbsent(cacheKey, k -> {
            try {
                String sql = "SELECT brand_code FROM brand WHERE brand_id = ?";
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setInt(1, brandId);
                    ResultSet rs = stmt.executeQuery();

                    if (rs.next()) {
                        return rs.getString("brand_code");
                    } else {
                        throw new IllegalArgumentException("Brand not found with ID: " + brandId);
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to get brand code", e);
            }
        });
    }

    /**
     * Clears the internal cache. Useful for testing or when master data changes.
     */
    public void clearCache() {
        categoryCodeCache.clear();
        subcategoryCodeCache.clear();
        brandCodeCache.clear();
    }

    /**
     * Gets a preview of what the product code would be without generating it.
     *
     * @param categoryId Category ID
     * @param subcategoryId Subcategory ID
     * @param brandId Brand ID
     * @return Preview of the product code format
     */
    public String previewProductCode(int categoryId, int subcategoryId, int brandId) {
        try {
            String categoryCode = getCategoryCode(categoryId);
            String subcategoryCode = getSubcategoryCode(subcategoryId);
            String brandCode = getBrandCode(brandId);
            int nextSequence = getNextSequenceNumber(categoryId, subcategoryId, brandId);

            return String.format("%s%s%s%03d", categoryCode, subcategoryCode, brandCode, nextSequence);

        } catch (SQLException e) {
            return "ERROR: Unable to preview code";
        }
    }
}