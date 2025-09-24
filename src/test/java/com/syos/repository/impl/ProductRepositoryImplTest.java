package com.syos.repository.impl;

import com.syos.domain.models.Product;
import com.syos.domain.valueobjects.ProductCode;
import com.syos.domain.valueobjects.Money;
import com.syos.domain.enums.UnitOfMeasure;
import com.syos.utils.DatabaseConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.*;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductRepositoryImplTest {

    private Connection connection;
    private PreparedStatement preparedStatement;
    private ResultSet resultSet;

    private ProductRepositoryImpl productRepository;
    private ProductCode testProductCode;
    private Money testUnitPrice;

    @BeforeEach
    void setUp() throws SQLException {
        testProductCode = new ProductCode("TEST001");
        testUnitPrice = new Money(new BigDecimal("15.99"));

        // Mock the database connection and related objects
        connection = mock(Connection.class);
        preparedStatement = mock(PreparedStatement.class);
        resultSet = mock(ResultSet.class);

        // Mock the DatabaseConnection singleton
        try (MockedStatic<DatabaseConnection> mockedDatabaseConnection = mockStatic(DatabaseConnection.class)) {
            DatabaseConnection databaseConnectionInstance = mock(DatabaseConnection.class);
            when(databaseConnectionInstance.getConnection()).thenReturn(connection);
            mockedDatabaseConnection.when(DatabaseConnection::getInstance).thenReturn(databaseConnectionInstance);

            // Create the repository instance
            productRepository = new ProductRepositoryImpl();
        }
    }

    @Test
    @DisplayName("Should find product by ID successfully")
    void shouldFindProductByIdSuccessfully() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        setupProductResultSet();

        // When
        Optional<Product> result = productRepository.findById(testProductCode);

        // Then
        assertTrue(result.isPresent());
        Product product = result.get();
        assertEquals("TEST001", product.getProductCode().getCode());
        assertEquals("Test Product", product.getProductName());
        verify(preparedStatement).setString(1, "TEST001");
    }

    @Test
    @DisplayName("Should return empty when product not found by ID")
    void shouldReturnEmptyWhenProductNotFoundById() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        // When
        Optional<Product> result = productRepository.findById(testProductCode);

        // Then
        assertFalse(result.isPresent());
        verify(preparedStatement).setString(1, "TEST001");
    }

    @Test
    @DisplayName("Should find active product by code successfully")
    void shouldFindActiveProductByCodeSuccessfully() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        setupProductResultSet();

        // When
        Optional<Product> result = productRepository.findByCodeAndActive(testProductCode);

        // Then
        assertTrue(result.isPresent());
        assertTrue(result.get().isActive());
        verify(preparedStatement).setString(1, "TEST001");
    }

    @Test
    @DisplayName("Should return empty when active product not found")
    void shouldReturnEmptyWhenActiveProductNotFound() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        // When
        Optional<Product> result = productRepository.findByCodeAndActive(testProductCode);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should search products by term successfully")
    void shouldSearchProductsByTermSuccessfully() throws SQLException {
        // Given
        String searchTerm = "test";
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, false);
        setupProductResultSet();

        // When
        List<Product> result = productRepository.searchByTerm(searchTerm);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(preparedStatement, times(5)).setString(anyInt(), eq("%test%"));
    }

    @Test
    @DisplayName("Should return empty list when no products match search term")
    void shouldReturnEmptyListWhenNoProductsMatchSearchTerm() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        // When
        List<Product> result = productRepository.searchByTerm("nonexistent");

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should find all active products successfully")
    void shouldFindAllActiveProductsSuccessfully() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, false);
        setupProductResultSet();

        // When
        List<Product> result = productRepository.findActiveProducts();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        // Verify all products are active
        assertTrue(result.stream().allMatch(Product::isActive));
    }

    @Test
    @DisplayName("Should find all products successfully")
    void shouldFindAllProductsSuccessfully() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, false);
        setupProductResultSet();

        // When
        List<Product> result = productRepository.findAll();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
    }

//    @Test
//    @DisplayName("Should save new product successfully")
//    void shouldSaveNewProductSuccessfully() throws SQLException {
//        // Given
//        Product product = createTestProduct();
//        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
//        when(preparedStatement.executeUpdate()).thenReturn(1);
//
//        // Mock existsById to return false (new product)
//        when(preparedStatement.executeQuery()).thenReturn(resultSet);
//        when(resultSet.next()).thenReturn(false);
//
//        // When
//        Product result = productRepository.save(product);
//
//        // Then
//        assertNotNull(result);
//        assertEquals(product, result);
//        verify(preparedStatement).setString(1, "TEST001");
//        verify(preparedStatement).setString(2, "Test Product");
//        verify(preparedStatement).setInt(3, 1);
//        verify(preparedStatement).setInt(4, 1);
//        verify(preparedStatement).setInt(5, 1);
//        verify(preparedStatement).setBigDecimal(6, new BigDecimal("15.99"));
//        verify(preparedStatement).setString(7, "Test Description");
//        verify(preparedStatement).setString(8, "pcs");
//        verify(preparedStatement).setBoolean(9, true);
//        verify(preparedStatement).executeUpdate();
//    }

    @Test
    @DisplayName("Should update existing product successfully")
    void shouldUpdateExistingProductSuccessfully() throws SQLException {
        // Given
        Product product = createTestProduct();
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        // Mock existsById to return true (existing product)
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);

        // When
        Product result = productRepository.save(product);

        // Then
        assertNotNull(result);
        assertEquals(product, result);
        verify(preparedStatement).setString(1, "Test Product");
        verify(preparedStatement).setInt(2, 1);
        verify(preparedStatement).setInt(3, 1);
        verify(preparedStatement).setInt(4, 1);
        verify(preparedStatement).setBigDecimal(5, new BigDecimal("15.99"));
        verify(preparedStatement).setString(6, "Test Description");
        verify(preparedStatement).setString(7, "pcs");
        verify(preparedStatement).setBoolean(8, true);
        verify(preparedStatement).setString(9, "TEST001");
        verify(preparedStatement).executeUpdate();
    }

    @Test
    @DisplayName("Should throw exception when inserting product fails")
    void shouldThrowExceptionWhenInsertingProductFails() throws SQLException {
        // Given
        Product product = createTestProduct();
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(0);

        // Mock existsById to return false
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> productRepository.save(product));
        assertTrue(exception.getMessage().contains("Error inserting product"));
    }

    @Test
    @DisplayName("Should throw exception when updating product fails")
    void shouldThrowExceptionWhenUpdatingProductFails() throws SQLException {
        // Given
        Product product = createTestProduct();
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(0);

        // Mock existsById to return true
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> productRepository.save(product));
        assertTrue(exception.getMessage().contains("Error updating product"));
    }

    @Test
    @DisplayName("Should delete product successfully (soft delete)")
    void shouldDeleteProductSuccessfully() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        // When
        productRepository.delete(testProductCode);

        // Then
        verify(preparedStatement).setString(1, "TEST001");
        verify(preparedStatement).executeUpdate();
    }

    @Test
    @DisplayName("Should check if product exists successfully")
    void shouldCheckIfProductExistsSuccessfully() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);

        // When
        boolean exists = productRepository.existsById(testProductCode);

        // Then
        assertTrue(exists);
        verify(preparedStatement).setString(1, "TEST001");
    }

    @Test
    @DisplayName("Should return false when product does not exist")
    void shouldReturnFalseWhenProductDoesNotExist() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        // When
        boolean exists = productRepository.existsById(testProductCode);

        // Then
        assertFalse(exists);
    }

    @Test
    @DisplayName("Should handle SQLException when finding product by ID")
    void shouldHandleSQLExceptionWhenFindingProductById() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("Database error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> productRepository.findById(testProductCode));
        assertTrue(exception.getMessage().contains("Error finding product by code"));
    }

    @Test
    @DisplayName("Should handle SQLException when searching products")
    void shouldHandleSQLExceptionWhenSearchingProducts() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("Database error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> productRepository.searchByTerm("test"));
        assertTrue(exception.getMessage().contains("Error searching products"));
    }

//    @Test
//    @DisplayName("Should handle SQLException when saving product")
//    void shouldHandleSQLExceptionWhenSavingProduct() throws SQLException {
//        // Given
//        Product product = createTestProduct();
//        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("Database error"));
//
//        // Mock existsById to return false
//        when(preparedStatement.executeQuery()).thenReturn(resultSet);
//        when(resultSet.next()).thenReturn(false);
//
//        // When & Then
//        RuntimeException exception = assertThrows(RuntimeException.class,
//                () -> productRepository.save(product));
//        assertTrue(exception.getMessage().contains("Error inserting product"));
//    }

    @Test
    @DisplayName("Should handle SQLException when deleting product")
    void shouldHandleSQLExceptionWhenDeletingProduct() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("Database error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> productRepository.delete(testProductCode));
        assertTrue(exception.getMessage().contains("Error deleting product"));
    }

    @Test
    @DisplayName("Should handle SQLException when checking product existence")
    void shouldHandleSQLExceptionWhenCheckingProductExistence() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("Database error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> productRepository.existsById(testProductCode));
        assertTrue(exception.getMessage().contains("Error checking product existence"));
    }

//    @Test
//    @DisplayName("Should map result set to product with all fields correctly")
//    void shouldMapResultSetToProductWithAllFieldsCorrectly() throws SQLException {
//        // Given
//        when(resultSet.getString("product_code")).thenReturn("TEST001");
//        when(resultSet.getString("product_name")).thenReturn("Test Product");
//        when(resultSet.getInt("category_id")).thenReturn(1);
//        when(resultSet.getInt("subcategory_id")).thenReturn(2);
//        when(resultSet.getInt("brand_id")).thenReturn(3);
//        when(resultSet.getBigDecimal("unit_price")).thenReturn(new BigDecimal("15.99"));
//        when(resultSet.getString("description")).thenReturn("Test Description");
//        when(resultSet.getString("unit_of_measure")).thenReturn("kg");
//        when(resultSet.getBoolean("is_active")).thenReturn(true);
//
//        // When
//        Product product = productRepository.findById(testProductCode).orElse(null);
//
//        // This test is mainly to verify the mapping logic works correctly
//        // We need to simulate the actual database call
//        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
//        when(preparedStatement.executeQuery()).thenReturn(resultSet);
//        when(resultSet.next()).thenReturn(true);
//
//        // Then
//        Optional<Product> result = productRepository.findById(testProductCode);
//        assertTrue(result.isPresent());
//        Product mappedProduct = result.get();
//
//        assertEquals("TEST001", mappedProduct.getProductCode().getCode());
//        assertEquals("Test Product", mappedProduct.getProductName());
//        assertEquals(1, mappedProduct.getCategoryId());
//        assertEquals(2, mappedProduct.getSubcategoryId());
//        assertEquals(3, mappedProduct.getBrandId());
//        assertEquals(new BigDecimal("15.99"), mappedProduct.getUnitPrice().getAmount());
//        assertEquals("Test Description", mappedProduct.getDescription());
//        assertEquals(UnitOfMeasure.KG, mappedProduct.getUnitOfMeasure());
//        assertTrue(mappedProduct.isActive());
//    }

    @Test
    @DisplayName("Should handle different unit of measure cases correctly")
    void shouldHandleDifferentUnitOfMeasureCasesCorrectly() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);

        when(resultSet.getString("product_code")).thenReturn("TEST001");
        when(resultSet.getString("product_name")).thenReturn("Test Product");
        when(resultSet.getInt("category_id")).thenReturn(1);
        when(resultSet.getInt("subcategory_id")).thenReturn(1);
        when(resultSet.getInt("brand_id")).thenReturn(1);
        when(resultSet.getBigDecimal("unit_price")).thenReturn(new BigDecimal("10.00"));
        when(resultSet.getString("description")).thenReturn("Test");
        when(resultSet.getString("unit_of_measure")).thenReturn("LITER"); // Uppercase in DB
        when(resultSet.getBoolean("is_active")).thenReturn(true);

        // When
        Optional<Product> result = productRepository.findById(testProductCode);

        // Then
        assertTrue(result.isPresent());
        assertEquals(UnitOfMeasure.LITER, result.get().getUnitOfMeasure());
    }

    @Test
    @DisplayName("Should return empty list when no active products found")
    void shouldReturnEmptyListWhenNoActiveProductsFound() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        // When
        List<Product> result = productRepository.findActiveProducts();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return empty list when no products found")
    void shouldReturnEmptyListWhenNoProductsFound() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        // When
        List<Product> result = productRepository.findAll();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should handle product with null description correctly")
    void shouldHandleProductWithNullDescriptionCorrectly() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);

        when(resultSet.getString("product_code")).thenReturn("TEST001");
        when(resultSet.getString("product_name")).thenReturn("Test Product");
        when(resultSet.getInt("category_id")).thenReturn(1);
        when(resultSet.getInt("subcategory_id")).thenReturn(1);
        when(resultSet.getInt("brand_id")).thenReturn(1);
        when(resultSet.getBigDecimal("unit_price")).thenReturn(new BigDecimal("10.00"));
        when(resultSet.getString("description")).thenReturn(null); // Null description
        when(resultSet.getString("unit_of_measure")).thenReturn("pcs");
        when(resultSet.getBoolean("is_active")).thenReturn(true);

        // When
        Optional<Product> result = productRepository.findById(testProductCode);

        // Then
        assertTrue(result.isPresent());
        assertNull(result.get().getDescription());
    }

    // Helper method to setup ResultSet for Product
    private void setupProductResultSet() throws SQLException {
        when(resultSet.getString("product_code")).thenReturn("TEST001");
        when(resultSet.getString("product_name")).thenReturn("Test Product");
        when(resultSet.getInt("category_id")).thenReturn(1);
        when(resultSet.getInt("subcategory_id")).thenReturn(1);
        when(resultSet.getInt("brand_id")).thenReturn(1);
        when(resultSet.getBigDecimal("unit_price")).thenReturn(new BigDecimal("15.99"));
        when(resultSet.getString("description")).thenReturn("Test Description");
        when(resultSet.getString("unit_of_measure")).thenReturn("pcs");
        when(resultSet.getBoolean("is_active")).thenReturn(true);
    }

    // Helper method to create a test product
    private Product createTestProduct() {
        return new Product(
                new ProductCode("TEST001"),
                "Test Product",
                1,
                1,
                1,
                new Money(new BigDecimal("15.99")),
                "Test Description",
                UnitOfMeasure.PCS,
                true
        );
    }
}