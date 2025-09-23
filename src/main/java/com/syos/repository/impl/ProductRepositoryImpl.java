package com.syos.repository.impl;

import com.syos.domain.models.Product;
import com.syos.domain.valueobjects.ProductCode;
import com.syos.domain.valueobjects.Money;
import com.syos.domain.enums.UnitOfMeasure;
import com.syos.repository.interfaces.ProductRepository;
import com.syos.utils.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductRepositoryImpl implements ProductRepository {
    private final Connection connection;

    public ProductRepositoryImpl() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    @Override
    public Optional<Product> findById(ProductCode productCode) {
        String sql = """
            SELECT p.product_code, p.product_name, p.category_id, p.subcategory_id,
                   p.brand_id, p.unit_price, p.description, p.unit_of_measure, p.is_active
            FROM product p
            WHERE p.product_code = ?
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, productCode.getCode());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToProduct(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding product by code", e);
        }

        return Optional.empty();
    }

    @Override
    public Optional<Product> findByCodeAndActive(ProductCode productCode) {
        String sql = """
            SELECT p.product_code, p.product_name, p.category_id, p.subcategory_id,
                   p.brand_id, p.unit_price, p.description, p.unit_of_measure, p.is_active
            FROM product p
            WHERE p.product_code = ? AND p.is_active = TRUE
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, productCode.getCode());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToProduct(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding active product by code", e);
        }

        return Optional.empty();
    }

    @Override
    public List<Product> searchByTerm(String searchTerm) {
        String sql = """
            SELECT p.product_code, p.product_name, p.category_id, p.subcategory_id,
                   p.brand_id, p.unit_price, p.description, p.unit_of_measure, p.is_active,
                   c.category_name, sc.subcategory_name, b.brand_name
            FROM product p
            JOIN category c ON p.category_id = c.category_id
            JOIN subcategory sc ON p.subcategory_id = sc.subcategory_id
            JOIN brand b ON p.brand_id = b.brand_id
            WHERE p.is_active = TRUE
            AND (p.product_code LIKE ? OR p.product_name LIKE ?
                 OR c.category_name LIKE ? OR sc.subcategory_name LIKE ?
                 OR b.brand_name LIKE ?)
            ORDER BY p.product_code
        """;

        List<Product> products = new ArrayList<>();
        String searchPattern = "%" + searchTerm + "%";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setString(3, searchPattern);
            stmt.setString(4, searchPattern);
            stmt.setString(5, searchPattern);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error searching products", e);
        }

        return products;
    }

    @Override
    public List<Product> findActiveProducts() {
        String sql = """
            SELECT p.product_code, p.product_name, p.category_id, p.subcategory_id,
                   p.brand_id, p.unit_price, p.description, p.unit_of_measure, p.is_active
            FROM product p
            WHERE p.is_active = TRUE
            ORDER BY p.product_code
        """;

        List<Product> products = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding active products", e);
        }

        return products;
    }

    @Override
    public List<Product> findAll() {
        String sql = """
            SELECT p.product_code, p.product_name, p.category_id, p.subcategory_id,
                   p.brand_id, p.unit_price, p.description, p.unit_of_measure, p.is_active
            FROM product p
            ORDER BY p.product_code
        """;

        List<Product> products = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding all products", e);
        }

        return products;
    }

    @Override
    public Product save(Product product) {
        if (existsById(product.getProductCode())) {
            return updateProduct(product);
        } else {
            return insertProduct(product);
        }
    }

    private Product insertProduct(Product product) {
        String sql = """
            INSERT INTO product (product_code, product_name, category_id, subcategory_id,
                               brand_id, unit_price, description, unit_of_measure, is_active)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, product.getProductCode().getCode());
            stmt.setString(2, product.getProductName());
            stmt.setInt(3, product.getCategoryId());
            stmt.setInt(4, product.getSubcategoryId());
            stmt.setInt(5, product.getBrandId());
            stmt.setBigDecimal(6, product.getUnitPrice().getAmount());
            stmt.setString(7, product.getDescription());
            stmt.setString(8, product.getUnitOfMeasure().name().toLowerCase());
            stmt.setBoolean(9, product.isActive());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                return product;
            } else {
                throw new SQLException("Creating product failed, no rows affected");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error inserting product", e);
        }
    }

    private Product updateProduct(Product product) {
        String sql = """
            UPDATE product 
            SET product_name = ?, category_id = ?, subcategory_id = ?, brand_id = ?,
                unit_price = ?, description = ?, unit_of_measure = ?, is_active = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE product_code = ?
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, product.getProductName());
            stmt.setInt(2, product.getCategoryId());
            stmt.setInt(3, product.getSubcategoryId());
            stmt.setInt(4, product.getBrandId());
            stmt.setBigDecimal(5, product.getUnitPrice().getAmount());
            stmt.setString(6, product.getDescription());
            stmt.setString(7, product.getUnitOfMeasure().name().toLowerCase());
            stmt.setBoolean(8, product.isActive());
            stmt.setString(9, product.getProductCode().getCode());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                return product;
            } else {
                throw new SQLException("Updating product failed, no rows affected");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error updating product", e);
        }
    }

    @Override
    public void delete(ProductCode productCode) {
        // Soft delete - just mark as inactive
        String sql = """
            UPDATE product 
            SET is_active = FALSE, updated_at = CURRENT_TIMESTAMP
            WHERE product_code = ?
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, productCode.getCode());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting product", e);
        }
    }

    @Override
    public boolean existsById(ProductCode productCode) {
        String sql = "SELECT 1 FROM product WHERE product_code = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, productCode.getCode());
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            throw new RuntimeException("Error checking product existence", e);
        }
    }

    private Product mapResultSetToProduct(ResultSet rs) throws SQLException {
        return new Product(
                new ProductCode(rs.getString("product_code")),
                rs.getString("product_name"),
                rs.getInt("category_id"),
                rs.getInt("subcategory_id"),
                rs.getInt("brand_id"),
                new Money(rs.getBigDecimal("unit_price")),
                rs.getString("description"),
                UnitOfMeasure.valueOf(rs.getString("unit_of_measure").toUpperCase()),
                rs.getBoolean("is_active")
        );
    }
}