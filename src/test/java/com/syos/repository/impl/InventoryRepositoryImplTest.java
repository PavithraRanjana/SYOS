package com.syos.repository.impl;

import com.syos.domain.models.MainInventory;
import com.syos.domain.models.PhysicalStoreInventory;
import com.syos.domain.valueobjects.ProductCode;
import com.syos.domain.valueobjects.Money;
import com.syos.repository.interfaces.InventoryRepository;
import com.syos.utils.DatabaseConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryRepositoryImplTest {

    private Connection connection;
    private PreparedStatement preparedStatement;
    private ResultSet resultSet;
    private Statement statement;

    private InventoryRepositoryImpl inventoryRepository;
    private ProductCode testProductCode;
    private Money testMoney;

    @BeforeEach
    void setUp() throws SQLException {
        testProductCode = new ProductCode("TEST001");
        testMoney = new Money(new BigDecimal("10.50"));

        // Mock the database connection and related objects
        connection = mock(Connection.class);
        preparedStatement = mock(PreparedStatement.class);
        resultSet = mock(ResultSet.class);
        statement = mock(Statement.class);

        // Mock the DatabaseConnection singleton
        try (MockedStatic<DatabaseConnection> mockedDatabaseConnection = mockStatic(DatabaseConnection.class)) {
            DatabaseConnection databaseConnectionInstance = mock(DatabaseConnection.class);
            when(databaseConnectionInstance.getConnection()).thenReturn(connection);
            mockedDatabaseConnection.when(DatabaseConnection::getInstance).thenReturn(databaseConnectionInstance);

            // Create the repository instance
            inventoryRepository = new InventoryRepositoryImpl();
        }
    }

    @Test
    @DisplayName("Should find physical store stock successfully")
    void shouldFindPhysicalStoreStockSuccessfully() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getInt("physical_inventory_id")).thenReturn(1, 2);
        when(resultSet.getString("product_code")).thenReturn("TEST001");
        when(resultSet.getInt("main_inventory_id")).thenReturn(100, 101);
        when(resultSet.getInt("quantity_on_shelf")).thenReturn(10, 5);
        when(resultSet.getDate("restocked_date")).thenReturn(Date.valueOf(LocalDate.now()));

        // When
        List<PhysicalStoreInventory> result = inventoryRepository.findPhysicalStoreStock(testProductCode);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(preparedStatement).setString(1, "TEST001");
        verify(preparedStatement).executeQuery();
    }

    @Test
    @DisplayName("Should return empty list when no physical store stock found")
    void shouldReturnEmptyListWhenNoPhysicalStoreStockFound() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        // When
        List<PhysicalStoreInventory> result = inventoryRepository.findPhysicalStoreStock(testProductCode);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should find physical store stock by batch successfully")
    void shouldFindPhysicalStoreStockByBatchSuccessfully() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("physical_inventory_id")).thenReturn(1);
        when(resultSet.getString("product_code")).thenReturn("TEST001");
        when(resultSet.getInt("main_inventory_id")).thenReturn(100);
        when(resultSet.getInt("quantity_on_shelf")).thenReturn(10);
        when(resultSet.getDate("restocked_date")).thenReturn(Date.valueOf(LocalDate.now()));

        // When
        Optional<PhysicalStoreInventory> result = inventoryRepository.findPhysicalStoreStockByBatch(testProductCode, 100);

        // Then
        assertTrue(result.isPresent());
        assertEquals(1, result.get().getPhysicalInventoryId());
        verify(preparedStatement).setString(1, "TEST001");
        verify(preparedStatement).setInt(2, 100);
    }

    @Test
    @DisplayName("Should return empty when physical store stock by batch not found")
    void shouldReturnEmptyWhenPhysicalStoreStockByBatchNotFound() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        // When
        Optional<PhysicalStoreInventory> result = inventoryRepository.findPhysicalStoreStockByBatch(testProductCode, 100);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should find next available batch successfully")
    void shouldFindNextAvailableBatchSuccessfully() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        setupMainInventoryResultSet();

        // When
        Optional<MainInventory> result = inventoryRepository.findNextAvailableBatch(testProductCode, 5);

        // Then
        assertTrue(result.isPresent());
        verify(preparedStatement).setString(1, "TEST001");
        verify(preparedStatement).setInt(2, 5);
    }

    @Test
    @DisplayName("Should find next available online batch successfully")
    void shouldFindNextAvailableOnlineBatchSuccessfully() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        setupMainInventoryResultSet();

        // When
        Optional<MainInventory> result = inventoryRepository.findNextAvailableOnlineBatch(testProductCode, 5);

        // Then
        assertTrue(result.isPresent());
        verify(preparedStatement).setString(1, "TEST001");
        verify(preparedStatement).setInt(2, 5);
    }

    @Test
    @DisplayName("Should find main inventory batches successfully")
    void shouldFindMainInventoryBatchesSuccessfully() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, false);
        setupMainInventoryResultSet();

        // When
        List<MainInventory> result = inventoryRepository.findMainInventoryBatches(testProductCode);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(preparedStatement).setString(1, "TEST001");
    }

    @Test
    @DisplayName("Should update physical store stock successfully")
    void shouldUpdatePhysicalStoreStockSuccessfully() throws SQLException {
        // Given
        PhysicalStoreInventory inventory = new PhysicalStoreInventory(1, testProductCode, 100, 15, LocalDate.now());
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        // When
        inventoryRepository.updatePhysicalStoreStock(inventory);

        // Then
        verify(preparedStatement).setInt(1, 15);
        verify(preparedStatement).setInt(2, 1);
        verify(preparedStatement).executeUpdate();
    }

    @Test
    @DisplayName("Should update main inventory stock successfully")
    void shouldUpdateMainInventoryStockSuccessfully() throws SQLException {
        // Given
        MainInventory inventory = new MainInventory(100, testProductCode, 50, testMoney,
                LocalDate.now(), LocalDate.now().plusDays(30), "Supplier", 25);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        // When
        inventoryRepository.updateMainInventoryStock(inventory);

        // Then
        verify(preparedStatement).setInt(1, 25);
        verify(preparedStatement).setInt(2, 100);
        verify(preparedStatement).executeUpdate();
    }

    @Test
    @DisplayName("Should reduce online stock successfully")
    void shouldReduceOnlineStockSuccessfully() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        // When
        inventoryRepository.reduceOnlineStock(testProductCode, 100, 5);

        // Then
        verify(preparedStatement).setInt(1, 5);
        verify(preparedStatement).setString(2, "TEST001");
        verify(preparedStatement).setInt(3, 100);
        verify(preparedStatement).setInt(4, 5);
        verify(preparedStatement).executeUpdate();
    }

    @Test
    @DisplayName("Should throw exception when reducing online stock fails")
    void shouldThrowExceptionWhenReducingOnlineStockFails() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(0);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> inventoryRepository.reduceOnlineStock(testProductCode, 100, 5));
        assertTrue(exception.getMessage().contains("Failed to reduce online stock"));
    }

    @Test
    @DisplayName("Should get total physical stock successfully")
    void shouldGetTotalPhysicalStockSuccessfully() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("total_stock")).thenReturn(25);

        // When
        int result = inventoryRepository.getTotalPhysicalStock(testProductCode);

        // Then
        assertEquals(25, result);
        verify(preparedStatement).setString(1, "TEST001");
    }

    @Test
    @DisplayName("Should get total online stock successfully")
    void shouldGetTotalOnlineStockSuccessfully() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("total_stock")).thenReturn(30);

        // When
        int result = inventoryRepository.getTotalOnlineStock(testProductCode);

        // Then
        assertEquals(30, result);
        verify(preparedStatement).setString(1, "TEST001");
    }

    @Test
    @DisplayName("Should add new batch successfully")
    void shouldAddNewBatchSuccessfully() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString(), anyInt())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(preparedStatement.getGeneratedKeys()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(100);

        LocalDate purchaseDate = LocalDate.now();
        LocalDate expiryDate = LocalDate.now().plusDays(30);

        // When
        MainInventory result = inventoryRepository.addNewBatch(testProductCode, 50, testMoney,
                purchaseDate, expiryDate, "Test Supplier");

        // Then
        assertNotNull(result);
        assertEquals(100, result.getBatchNumber());
        verify(preparedStatement).setString(1, "TEST001");
        verify(preparedStatement).setInt(2, 50);
        verify(preparedStatement).setBigDecimal(3, new BigDecimal("10.50"));
        verify(preparedStatement).setDate(4, Date.valueOf(purchaseDate));
        verify(preparedStatement).setDate(5, Date.valueOf(expiryDate));
        verify(preparedStatement).setString(6, "Test Supplier");
        verify(preparedStatement).setInt(7, 50);
    }

    @Test
    @DisplayName("Should throw exception when adding new batch fails")
    void shouldThrowExceptionWhenAddingNewBatchFails() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString(), anyInt())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(0);

        LocalDate purchaseDate = LocalDate.now();
        LocalDate expiryDate = LocalDate.now().plusDays(30);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> inventoryRepository.addNewBatch(testProductCode, 50, testMoney,
                        purchaseDate, expiryDate, "Test Supplier"));
        assertTrue(exception.getMessage().contains("Error adding new batch"));
    }

    @Test
    @DisplayName("Should find batch by number successfully")
    void shouldFindBatchByNumberSuccessfully() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        setupMainInventoryResultSet();

        // When
        Optional<MainInventory> result = inventoryRepository.findBatchByNumber(100);

        // Then
        assertTrue(result.isPresent());
        verify(preparedStatement).setInt(1, 100);
    }

    @Test
    @DisplayName("Should remove batch successfully")
    void shouldRemoveBatchSuccessfully() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        // When
        inventoryRepository.removeBatch(100);

        // Then
        verify(preparedStatement).setInt(1, 100);
        verify(preparedStatement).executeUpdate();
    }

    @Test
    @DisplayName("Should throw exception when removing non-existent batch")
    void shouldThrowExceptionWhenRemovingNonExistentBatch() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(0);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> inventoryRepository.removeBatch(100));
        assertTrue(exception.getMessage().contains("Batch not found"));
    }

    @Test
    @DisplayName("Should issue to physical store successfully - update existing")
    void shouldIssueToPhysicalStoreSuccessfullyUpdateExisting() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true); // Record exists
        when(preparedStatement.executeUpdate()).thenReturn(1);

        // When
        inventoryRepository.issueToPhysicalStore(testProductCode, 100, 10);

        // Then
        verify(connection).setAutoCommit(false);
        verify(connection).commit();
        verify(connection).setAutoCommit(true);
    }

    @Test
    @DisplayName("Should issue to physical store successfully - insert new")
    void shouldIssueToPhysicalStoreSuccessfullyInsertNew() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false); // Record doesn't exist
        when(preparedStatement.executeUpdate()).thenReturn(1);

        // When
        inventoryRepository.issueToPhysicalStore(testProductCode, 100, 10);

        // Then
        verify(connection).setAutoCommit(false);
        verify(connection).commit();
        verify(connection).setAutoCommit(true);
    }

    @Test
    @DisplayName("Should handle SQLException when issuing to physical store")
    void shouldHandleSQLExceptionWhenIssuingToPhysicalStore() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("Test error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> inventoryRepository.issueToPhysicalStore(testProductCode, 100, 10));
        assertTrue(exception.getMessage().contains("Error issuing to physical store"));
        verify(connection, atLeastOnce()).rollback();
    }

    @Test
    @DisplayName("Should return from physical store successfully")
    void shouldReturnFromPhysicalStoreSuccessfully() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        // When
        inventoryRepository.returnFromPhysicalStore(testProductCode, 100, 5);

        // Then
        verify(preparedStatement).setInt(1, 5);
        verify(preparedStatement).setString(2, "TEST001");
        verify(preparedStatement).setInt(3, 100);
        verify(preparedStatement).setInt(4, 5);
        verify(preparedStatement).executeUpdate();
    }

    @Test
    @DisplayName("Should reduce main inventory stock successfully")
    void shouldReduceMainInventoryStockSuccessfully() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        // When
        inventoryRepository.reduceMainInventoryStock(100, 5);

        // Then
        verify(preparedStatement).setInt(1, 5);
        verify(preparedStatement).setInt(2, 100);
        verify(preparedStatement).setInt(3, 5);
        verify(preparedStatement).executeUpdate();
    }

    @Test
    @DisplayName("Should get total main inventory stock successfully")
    void shouldGetTotalMainInventoryStockSuccessfully() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("total_stock")).thenReturn(100);

        // When
        int result = inventoryRepository.getTotalMainInventoryStock(testProductCode);

        // Then
        assertEquals(100, result);
        verify(preparedStatement).setString(1, "TEST001");
    }

    @Test
    @DisplayName("Should get physical store usage successfully")
    void shouldGetPhysicalStoreUsageSuccessfully() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("usage")).thenReturn(15);

        // When
        int result = inventoryRepository.getPhysicalStoreUsage(100);

        // Then
        assertEquals(15, result);
        verify(preparedStatement).setInt(1, 100);
    }

    @Test
    @DisplayName("Should get online store usage successfully")
    void shouldGetOnlineStoreUsageSuccessfully() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("usage")).thenReturn(20);

        // When
        int result = inventoryRepository.getOnlineStoreUsage(100);

        // Then
        assertEquals(20, result);
        verify(preparedStatement).setInt(1, 100);
    }

    @Test
    @DisplayName("Should return true when batch has been sold")
    void shouldReturnTrueWhenBatchHasBeenSold() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("sale_count")).thenReturn(3);

        // When
        boolean result = inventoryRepository.batchHasBeenSold(100);

        // Then
        assertTrue(result);
        verify(preparedStatement).setInt(1, 100);
    }

    @Test
    @DisplayName("Should return false when batch has not been sold")
    void shouldReturnFalseWhenBatchHasNotBeenSold() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("sale_count")).thenReturn(0);

        // When
        boolean result = inventoryRepository.batchHasBeenSold(100);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("Should find low stock batches successfully")
    void shouldFindLowStockBatchesSuccessfully() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, false);
        setupMainInventoryResultSet();

        // When
        List<MainInventory> result = inventoryRepository.findLowStockBatches(10);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(preparedStatement).setInt(1, 10);
    }

    @Test
    @DisplayName("Should find batches expiring before date successfully")
    void shouldFindBatchesExpiringBeforeDateSuccessfully() throws SQLException {
        // Given
        LocalDate beforeDate = LocalDate.now().plusDays(7);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        setupMainInventoryResultSet();

        // When
        List<MainInventory> result = inventoryRepository.findBatchesExpiringBefore(beforeDate);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(preparedStatement).setDate(1, Date.valueOf(beforeDate));
    }

    @Test
    @DisplayName("Should find all categories successfully")
    void shouldFindAllCategoriesSuccessfully() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getInt("category_id")).thenReturn(1, 2);
        when(resultSet.getString("category_name")).thenReturn("Electronics", "Clothing");
        when(resultSet.getString("category_code")).thenReturn("ELEC", "CLTH");

        // When
        List<InventoryRepository.CategoryData> result = inventoryRepository.findAllCategories();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Electronics", result.get(0).getCategoryName());
    }

    @Test
    @DisplayName("Should find subcategories by category successfully")
    void shouldFindSubcategoriesByCategorySuccessfully() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getInt("subcategory_id")).thenReturn(1);
        when(resultSet.getString("subcategory_name")).thenReturn("Laptops");
        when(resultSet.getString("subcategory_code")).thenReturn("LAP");
        when(resultSet.getInt("category_id")).thenReturn(1);

        // When
        List<InventoryRepository.SubcategoryData> result = inventoryRepository.findSubcategoriesByCategory(1);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Laptops", result.get(0).getSubcategoryName());
        verify(preparedStatement).setInt(1, 1);
    }

    @Test
    @DisplayName("Should find all brands successfully")
    void shouldFindAllBrandsSuccessfully() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getInt("brand_id")).thenReturn(1);
        when(resultSet.getString("brand_name")).thenReturn("Nike");
        when(resultSet.getString("brand_code")).thenReturn("NIKE");

        // When
        List<InventoryRepository.BrandData> result = inventoryRepository.findAllBrands();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Nike", result.get(0).getBrandName());
    }

    @Test
    @DisplayName("Should handle SQLException gracefully")
    void shouldHandleSQLExceptionGracefully() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("Database error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> inventoryRepository.findPhysicalStoreStock(testProductCode));
        assertTrue(exception.getMessage().contains("Error finding physical store stock"));
    }

    @Test
    @DisplayName("Should restore batch successfully")
    void shouldRestoreBatchSuccessfully() throws SQLException {
        // Given
        MainInventory batch = new MainInventory(100, testProductCode, 50, testMoney,
                LocalDate.now(), LocalDate.now().plusDays(30), "Supplier", 25);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        // When
        MainInventory result = inventoryRepository.restoreBatch(batch);

        // Then
        assertNotNull(result);
        assertEquals(batch, result);
        verify(preparedStatement).setInt(1, 100);
        verify(preparedStatement).setString(2, "TEST001");
        verify(preparedStatement).setInt(3, 50);
        verify(preparedStatement).setBigDecimal(4, new BigDecimal("10.50"));
        verify(preparedStatement).executeUpdate();
    }

    @Test
    @DisplayName("Should issue to online store successfully")
    void shouldIssueToOnlineStoreSuccessfully() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true); // Record exists
        when(preparedStatement.executeUpdate()).thenReturn(1);

        // When
        inventoryRepository.issueToOnlineStore(testProductCode, 100, 10);

        // Then
        verify(connection).setAutoCommit(false);
        verify(connection).commit();
        verify(connection).setAutoCommit(true);
    }

    @Test
    @DisplayName("Should return from online store successfully")
    void shouldReturnFromOnlineStoreSuccessfully() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        // When
        inventoryRepository.returnFromOnlineStore(testProductCode, 100, 5);

        // Then
        verify(preparedStatement).setInt(1, 5);
        verify(preparedStatement).setString(2, "TEST001");
        verify(preparedStatement).setInt(3, 100);
        verify(preparedStatement).setInt(4, 5);
        verify(preparedStatement).executeUpdate();
    }

    @Test
    @DisplayName("Should restore main inventory stock successfully")
    void shouldRestoreMainInventoryStockSuccessfully() throws SQLException {
        // Given
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        // When
        inventoryRepository.restoreMainInventoryStock(100, 10);

        // Then
        verify(preparedStatement).setInt(1, 10);
        verify(preparedStatement).setInt(2, 100);
        verify(preparedStatement).executeUpdate();
    }

    // Helper method to setup ResultSet for MainInventory
    private void setupMainInventoryResultSet() throws SQLException {
        when(resultSet.getInt("main_inventory_id")).thenReturn(100);
        when(resultSet.getString("product_code")).thenReturn("TEST001");
        when(resultSet.getInt("quantity_received")).thenReturn(50);
        when(resultSet.getBigDecimal("purchase_price")).thenReturn(new BigDecimal("10.50"));
        when(resultSet.getDate("purchase_date")).thenReturn(Date.valueOf(LocalDate.now()));
        when(resultSet.getDate("expiry_date")).thenReturn(Date.valueOf(LocalDate.now().plusDays(30)));
        when(resultSet.getString("supplier_name")).thenReturn("Test Supplier");
        when(resultSet.getInt("remaining_quantity")).thenReturn(25);
    }

//    // Test for the helper method mapResultSetToMainInventory
//    @Test
//    @DisplayName("Should map result set to MainInventory successfully")
//    void shouldMapResultSetToMainInventorySuccessfully() throws SQLException, NoSuchFieldException, IllegalAccessException {
//        // Given
//        when(resultSet.getInt("main_inventory_id")).thenReturn(100);
//        when(resultSet.getString("product_code")).thenReturn("TEST001");
//        when(resultSet.getInt("quantity_received")).thenReturn(50);
//        when(resultSet.getBigDecimal("purchase_price")).thenReturn(new BigDecimal("10.50"));
//        when(resultSet.getDate("purchase_date")).thenReturn(Date.valueOf(LocalDate.now()));
//        when(resultSet.getDate("expiry_date")).thenReturn(Date.valueOf(LocalDate.now().plusDays(30)));
//        when(resultSet.getString("supplier_name")).thenReturn("Test Supplier");
//        when(resultSet.getInt("remaining_quantity")).thenReturn(25);
//
//        // Use reflection to access the private method
//        Field repositoryField = InventoryRepositoryImpl.class.getDeclaredField("connection");
//        repositoryField.setAccessible(true);
//        repositoryField.set(inventoryRepository, connection);
//
//        // When
//        MainInventory result = inventoryRepository.findBatchByNumber(100).orElse(null);
//
//        // Then
//        assertNotNull(result);
//        assertEquals(100, result.getBatchNumber());
//        assertEquals("TEST001", result.getProductCode().getCode());
//        assertEquals(50, result.getQuantityReceived());
//        assertEquals(25, result.getRemainingQuantity());
//    }
}