package com.syos.service.impl;

import com.syos.domain.valueobjects.ProductCode;
import com.syos.utils.DatabaseConnection;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class ProductCodeGeneratorImplTest {

    private ProductCodeGeneratorImpl codeGenerator;
    private Connection mockConnection;
    private PreparedStatement mockStatement;
    private ResultSet mockResultSet;
    private MockedStatic<DatabaseConnection> mockedDbConnection;

    @BeforeEach
    void setUp() {
        // Simple mock setup
        mockConnection = mock(Connection.class);
        mockStatement = mock(PreparedStatement.class);
        mockResultSet = mock(ResultSet.class);

        DatabaseConnection mockDbInstance = mock(DatabaseConnection.class);
        mockedDbConnection = mockStatic(DatabaseConnection.class);
        mockedDbConnection.when(DatabaseConnection::getInstance).thenReturn(mockDbInstance);
        when(mockDbInstance.getConnection()).thenReturn(mockConnection);

        codeGenerator = new ProductCodeGeneratorImpl();
    }

    @AfterEach
    void tearDown() {
        mockedDbConnection.close();
    }

    @Test
    @DisplayName("Should generate product code successfully - Happy Path")
    void shouldGenerateProductCode() throws SQLException {
        // Given - Simple mock setup for successful code generation
        setupSuccessfulMocks("FOD", "SNK", "COK", 1);

        // When
        ProductCode result = codeGenerator.generateProductCode(1, 2, 3);

        // Then
        assertNotNull(result);
        assertEquals("FODSNKCOK001", result.getCode());
    }

    @Test
    @DisplayName("Should generate code with correct sequence number")
    void shouldGenerateCodeWithSequenceNumber() throws SQLException {
        // Given
        setupSuccessfulMocks("BEV", "SFT", "PEP", 25);

        // When
        ProductCode result = codeGenerator.generateProductCode(1, 2, 3);

        // Then
        assertEquals("BEVSFTPEP025", result.getCode());
    }

    @Test
    @DisplayName("Should get next sequence number correctly")
    void shouldGetNextSequenceNumber() throws SQLException {
        // Given
        setupSuccessfulMocks("CAT", "SUB", "BRD", 5);

        // When
        int sequence = codeGenerator.getNextSequenceNumber(1, 2, 3);

        // Then
        assertEquals(5, sequence);
    }

    @Test
    @DisplayName("Should return 1 for first product in category")
    void shouldReturnOneForFirstProduct() throws SQLException {
        // Given - Set up all required mocks
        setupMocksForCodes("CAT", "SUB", "BRD");

        // Mock the sequence query to return no results (no existing products)
        PreparedStatement mockSequenceStmt = mock(PreparedStatement.class);
        ResultSet mockSequenceResult = mock(ResultSet.class);

        when(mockConnection.prepareStatement(contains("COALESCE")))
                .thenReturn(mockSequenceStmt);
        when(mockSequenceStmt.executeQuery()).thenReturn(mockSequenceResult);
        when(mockSequenceResult.next()).thenReturn(false); // No existing products

        // When
        int sequence = codeGenerator.getNextSequenceNumber(1, 2, 3);

        // Then
        assertEquals(1, sequence);

        // Verify the sequence query was executed with correct pattern
        verify(mockSequenceStmt).setString(1, "CATSUBBRD%");
    }

    @Test
    @DisplayName("Should use cache on repeated calls")
    void shouldUseCacheOnRepeatedCalls() throws SQLException {
        // Given
        setupSuccessfulMocks("CAT", "SUB", "BRD", 1);

        // When - First call
        codeGenerator.generateProductCode(1, 2, 3);

        // Setup only sequence query for second call
        setupSequenceMock(mockResultSet -> {
            when(mockResultSet.next()).thenReturn(true);
            when(mockResultSet.getInt("next_sequence")).thenReturn(2);
        });

        // When - Second call
        ProductCode result = codeGenerator.generateProductCode(1, 2, 3);

        // Then
        assertEquals("CATSUBBRD002", result.getCode());
        // Verify category/subcategory/brand queries were called only once (cached)
        verify(mockConnection, times(5)).prepareStatement(anyString()); // 4 first + 1 second
    }

    @Test
    @DisplayName("Should clear cache successfully")
    void shouldClearCache() throws SQLException {
        // Given - First call to populate cache
        setupSuccessfulMocks("TST", "CLR", "CHE", 1);
        codeGenerator.generateProductCode(1, 2, 3);

        // When
        codeGenerator.clearCache();

        // Then - Should make database calls again after cache clear
        setupSuccessfulMocks("TST", "CLR", "CHE", 2);
        codeGenerator.generateProductCode(1, 2, 3);

        // Verify all queries were made twice (cache was cleared)
        verify(mockConnection, times(8)).prepareStatement(anyString());
    }

    @Test
    @DisplayName("Should preview product code")
    void shouldPreviewProductCode() throws SQLException {
        // Given
        setupSuccessfulMocks("PRV", "TST", "BRD", 10);

        // When
        String preview = codeGenerator.previewProductCode(1, 2, 3);

        // Then
        assertEquals("PRVTSTBRD010", preview);
    }

    @Test
    @DisplayName("Should handle database error gracefully")
    void shouldHandleDatabaseError() throws SQLException {
        // Given - Database connection fails
        when(mockConnection.prepareStatement(anyString()))
                .thenThrow(new SQLException("Database error"));

        // When & Then - Should throw RuntimeException
        assertThrows(RuntimeException.class, () ->
                codeGenerator.generateProductCode(1, 2, 3));
    }

    @Test
    @DisplayName("Should handle category not found")
    void shouldHandleCategoryNotFound() throws SQLException {
        // Given - Category query returns no results
        when(mockConnection.prepareStatement(contains("category_code")))
                .thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false); // No category found

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                codeGenerator.generateProductCode(999, 2, 3));

        assertTrue(exception.getMessage().contains("Category not found"));
    }

    @Test
    @DisplayName("Should handle subcategory not found")
    void shouldHandleSubcategoryNotFound() throws SQLException {
        // Given - Category exists but subcategory doesn't
        PreparedStatement mockCategoryStmt = mock(PreparedStatement.class);
        PreparedStatement mockSubcategoryStmt = mock(PreparedStatement.class);
        ResultSet mockCategoryResult = mock(ResultSet.class);
        ResultSet mockSubcategoryResult = mock(ResultSet.class);

        // Setup category (exists)
        when(mockConnection.prepareStatement(contains("category_code")))
                .thenReturn(mockCategoryStmt);
        when(mockCategoryStmt.executeQuery()).thenReturn(mockCategoryResult);
        when(mockCategoryResult.next()).thenReturn(true);
        when(mockCategoryResult.getString("category_code")).thenReturn("CAT");

        // Setup subcategory (does not exist)
        when(mockConnection.prepareStatement(contains("subcategory_code")))
                .thenReturn(mockSubcategoryStmt);
        when(mockSubcategoryStmt.executeQuery()).thenReturn(mockSubcategoryResult);
        when(mockSubcategoryResult.next()).thenReturn(false); // No subcategory found

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                codeGenerator.generateProductCode(1, 999, 3));

        assertTrue(exception.getMessage().contains("Subcategory not found with ID: 999"),
                "Exception message should contain 'Subcategory not found'. Actual: " + exception.getMessage());
    }

    @Test
    @DisplayName("Should handle brand not found")
    void shouldHandleBrandNotFound() throws SQLException {
        // Given - Category and subcategory exist but brand doesn't
        PreparedStatement mockCategoryStmt = mock(PreparedStatement.class);
        PreparedStatement mockSubcategoryStmt = mock(PreparedStatement.class);
        PreparedStatement mockBrandStmt = mock(PreparedStatement.class);
        ResultSet mockCategoryResult = mock(ResultSet.class);
        ResultSet mockSubcategoryResult = mock(ResultSet.class);
        ResultSet mockBrandResult = mock(ResultSet.class);

        // Setup category (exists)
        when(mockConnection.prepareStatement(contains("category_code")))
                .thenReturn(mockCategoryStmt);
        when(mockCategoryStmt.executeQuery()).thenReturn(mockCategoryResult);
        when(mockCategoryResult.next()).thenReturn(true);
        when(mockCategoryResult.getString("category_code")).thenReturn("CAT");

        // Setup subcategory (exists)
        when(mockConnection.prepareStatement(contains("subcategory_code")))
                .thenReturn(mockSubcategoryStmt);
        when(mockSubcategoryStmt.executeQuery()).thenReturn(mockSubcategoryResult);
        when(mockSubcategoryResult.next()).thenReturn(true);
        when(mockSubcategoryResult.getString("subcategory_code")).thenReturn("SUB");

        // Setup brand (does not exist)
        when(mockConnection.prepareStatement(contains("brand_code")))
                .thenReturn(mockBrandStmt);
        when(mockBrandStmt.executeQuery()).thenReturn(mockBrandResult);
        when(mockBrandResult.next()).thenReturn(false); // No brand found

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                codeGenerator.generateProductCode(1, 2, 999));

        assertTrue(exception.getMessage().contains("Brand not found with ID: 999"));
    }

    // ========== Helper Methods (Simplified) ==========

    /**
     * Sets up all mocks for successful product code generation
     */
    private void setupSuccessfulMocks(String categoryCode, String subcategoryCode,
                                      String brandCode, int sequenceNumber) throws SQLException {
        setupMocksForCodes(categoryCode, subcategoryCode, brandCode);
        setupSequenceMock(mockResultSet -> {
            when(mockResultSet.next()).thenReturn(true);
            when(mockResultSet.getInt("next_sequence")).thenReturn(sequenceNumber);
        });
    }

    /**
     * Sets up mocks for category, subcategory, and brand codes
     */
    private void setupMocksForCodes(String categoryCode, String subcategoryCode, String brandCode) throws SQLException {
        setupCategoryMock(categoryCode);
        setupSubcategoryMock(subcategoryCode);
        setupBrandMock(brandCode);
    }

    /**
     * Sets up category code mock
     */
    private void setupCategoryMock(String categoryCode) throws SQLException {
        when(mockConnection.prepareStatement(contains("category_code")))
                .thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getString("category_code")).thenReturn(categoryCode);
    }

    /**
     * Sets up subcategory code mock
     */
    private void setupSubcategoryMock(String subcategoryCode) throws SQLException {
        when(mockConnection.prepareStatement(contains("subcategory_code")))
                .thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getString("subcategory_code")).thenReturn(subcategoryCode);
    }

    /**
     * Sets up brand code mock
     */
    private void setupBrandMock(String brandCode) throws SQLException {
        when(mockConnection.prepareStatement(contains("brand_code")))
                .thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getString("brand_code")).thenReturn(brandCode);
    }

    /**
     * Sets up sequence number mock with custom behavior
     */
    private void setupSequenceMock(MockConfig config) throws SQLException {
        when(mockConnection.prepareStatement(contains("COALESCE")))
                .thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        config.configure(mockResultSet);
    }

    /**
     * Functional interface for configuring mocks
     */
    @FunctionalInterface
    private interface MockConfig {
        void configure(ResultSet mockResultSet) throws SQLException;
    }
}