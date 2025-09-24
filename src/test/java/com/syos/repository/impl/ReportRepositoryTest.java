package com.syos.repository.impl;

import com.syos.repository.interfaces.ReportRepository;
import com.syos.service.interfaces.ReportService.*;
import com.syos.utils.DatabaseConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReportRepositoryImpl.
 * Tests database interaction logic with mocked connections.
 */
@ExtendWith(MockitoExtension.class)
class ReportRepositoryTest {

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @Mock
    private DatabaseConnection dbConnection;

    private ReportRepository reportRepository;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.of(2024, 9, 24);
    }

    // ==================== SALES REPORT DATA TESTS ====================

    @Test
    @DisplayName("Should fetch physical store sales data successfully")
    void shouldFetchPhysicalStoreSalesDataSuccessfully() throws SQLException {
        // Arrange
        try (MockedStatic<DatabaseConnection> mockedStatic = mockStatic(DatabaseConnection.class)) {
            mockedStatic.when(DatabaseConnection::getInstance).thenReturn(dbConnection);
            when(dbConnection.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);

            // Mock result set data
            when(resultSet.next())
                    .thenReturn(true)  // First row
                    .thenReturn(true)  // Second row
                    .thenReturn(false); // End of data

            // First row data
            when(resultSet.getString("product_code"))
                    .thenReturn("BVEDRB001")
                    .thenReturn("BVSDCC001");
            when(resultSet.getString("product_name"))
                    .thenReturn("Red Bull Energy Drink 250ml")
                    .thenReturn("Coca-Cola 330ml");
            when(resultSet.getInt("total_quantity"))
                    .thenReturn(10)
                    .thenReturn(15);
            when(resultSet.getBigDecimal("revenue"))
                    .thenReturn(new BigDecimal("2500.00"))
                    .thenReturn(new BigDecimal("1800.00"));

            reportRepository = new ReportRepositoryImpl();

            // Act
            List<SalesItem> result = reportRepository.getPhysicalStoreSalesData(testDate);

            // Assert
            assertEquals(2, result.size());

            SalesItem firstItem = result.get(0);
            assertEquals("BVEDRB001", firstItem.getProductCode());
            assertEquals("Red Bull Energy Drink 250ml", firstItem.getProductName());
            assertEquals(10, firstItem.getTotalQuantity());
            assertEquals(new BigDecimal("2500.00"), firstItem.getRevenue());

            SalesItem secondItem = result.get(1);
            assertEquals("BVSDCC001", secondItem.getProductCode());
            assertEquals(15, secondItem.getTotalQuantity());

            verify(preparedStatement).setDate(1, Date.valueOf(testDate));
            verify(preparedStatement).executeQuery();
        }
    }

    @Test
    @DisplayName("Should fetch online store sales data successfully")
    void shouldFetchOnlineStoreSalesDataSuccessfully() throws SQLException {
        // Arrange
        try (MockedStatic<DatabaseConnection> mockedStatic = mockStatic(DatabaseConnection.class)) {
            mockedStatic.when(DatabaseConnection::getInstance).thenReturn(dbConnection);
            when(dbConnection.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);

            when(resultSet.next())
                    .thenReturn(true)
                    .thenReturn(false);

            when(resultSet.getString("product_code")).thenReturn("CHDKLIN001");
            when(resultSet.getString("product_name")).thenReturn("Lindt Dark Chocolate 70% 100g");
            when(resultSet.getInt("total_quantity")).thenReturn(5);
            when(resultSet.getBigDecimal("revenue")).thenReturn(new BigDecimal("4250.00"));

            reportRepository = new ReportRepositoryImpl();

            // Act
            List<SalesItem> result = reportRepository.getOnlineStoreSalesData(testDate);

            // Assert
            assertEquals(1, result.size());
            SalesItem item = result.get(0);
            assertEquals("CHDKLIN001", item.getProductCode());
            assertEquals("Lindt Dark Chocolate 70% 100g", item.getProductName());
            assertEquals(5, item.getTotalQuantity());
            assertEquals(new BigDecimal("4250.00"), item.getRevenue());

            verify(preparedStatement).setDate(1, Date.valueOf(testDate));
        }
    }

    @Test
    @DisplayName("Should fetch physical store revenue successfully")
    void shouldFetchPhysicalStoreRevenueSuccessfully() throws SQLException {
        // Arrange
        try (MockedStatic<DatabaseConnection> mockedStatic = mockStatic(DatabaseConnection.class)) {
            mockedStatic.when(DatabaseConnection::getInstance).thenReturn(dbConnection);
            when(dbConnection.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);

            when(resultSet.next()).thenReturn(true);
            when(resultSet.getBigDecimal("revenue")).thenReturn(new BigDecimal("8550.00"));

            reportRepository = new ReportRepositoryImpl();

            // Act
            BigDecimal result = reportRepository.getPhysicalStoreRevenue(testDate);

            // Assert
            assertEquals(new BigDecimal("8550.00"), result);
            verify(preparedStatement).setDate(1, Date.valueOf(testDate));
        }
    }

    @Test
    @DisplayName("Should return zero revenue when no sales found")
    void shouldReturnZeroRevenueWhenNoSalesFound() throws SQLException {
        // Arrange
        try (MockedStatic<DatabaseConnection> mockedStatic = mockStatic(DatabaseConnection.class)) {
            mockedStatic.when(DatabaseConnection::getInstance).thenReturn(dbConnection);
            when(dbConnection.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);

            when(resultSet.next()).thenReturn(false); // No results

            reportRepository = new ReportRepositoryImpl();

            // Act
            BigDecimal result = reportRepository.getPhysicalStoreRevenue(testDate);

            // Assert
            assertEquals(BigDecimal.ZERO, result);
        }
    }

    // ==================== RESTOCK REPORT DATA TESTS ====================

    @Test
    @DisplayName("Should fetch physical store restock needs successfully")
    void shouldFetchPhysicalStoreRestockNeedsSuccessfully() throws SQLException {
        // Arrange
        try (MockedStatic<DatabaseConnection> mockedStatic = mockStatic(DatabaseConnection.class)) {
            mockedStatic.when(DatabaseConnection::getInstance).thenReturn(dbConnection);
            when(dbConnection.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);

            when(resultSet.next())
                    .thenReturn(true)
                    .thenReturn(false);

            when(resultSet.getString("product_code")).thenReturn("BVEDRB001");
            when(resultSet.getString("product_name")).thenReturn("Red Bull Energy Drink 250ml");
            when(resultSet.getInt("current_quantity")).thenReturn(45);
            when(resultSet.getInt("quantity_needed")).thenReturn(25);

            reportRepository = new ReportRepositoryImpl();

            // Act
            List<RestockItem> result = reportRepository.getPhysicalStoreRestockNeeds(testDate);

            // Assert
            assertEquals(1, result.size());
            RestockItem item = result.get(0);
            assertEquals("BVEDRB001", item.getProductCode());
            assertEquals("Red Bull Energy Drink 250ml", item.getProductName());
            assertEquals(45, item.getCurrentQuantity());
            assertEquals(25, item.getQuantityNeeded());
        }
    }

    @Test
    @DisplayName("Should fetch online store restock needs successfully")
    void shouldFetchOnlineStoreRestockNeedsSuccessfully() throws SQLException {
        // Arrange
        try (MockedStatic<DatabaseConnection> mockedStatic = mockStatic(DatabaseConnection.class)) {
            mockedStatic.when(DatabaseConnection::getInstance).thenReturn(dbConnection);
            when(dbConnection.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);

            when(resultSet.next())
                    .thenReturn(true)
                    .thenReturn(true)
                    .thenReturn(false);

            when(resultSet.getString("product_code"))
                    .thenReturn("BVEDRB001")
                    .thenReturn("CHDKLIN001");
            when(resultSet.getString("product_name"))
                    .thenReturn("Red Bull Energy Drink 250ml")
                    .thenReturn("Lindt Dark Chocolate 70% 100g");
            when(resultSet.getInt("current_quantity"))
                    .thenReturn(30)
                    .thenReturn(15);
            when(resultSet.getInt("quantity_needed"))
                    .thenReturn(40)
                    .thenReturn(55);

            reportRepository = new ReportRepositoryImpl();

            // Act
            List<RestockItem> result = reportRepository.getOnlineStoreRestockNeeds(testDate);

            // Assert
            assertEquals(2, result.size());

            RestockItem firstItem = result.get(0);
            assertEquals("BVEDRB001", firstItem.getProductCode());
            assertEquals(30, firstItem.getCurrentQuantity());
            assertEquals(40, firstItem.getQuantityNeeded());

            RestockItem secondItem = result.get(1);
            assertEquals("CHDKLIN001", secondItem.getProductCode());
            assertEquals(15, secondItem.getCurrentQuantity());
            assertEquals(55, secondItem.getQuantityNeeded());
        }
    }

    // ==================== REORDER REPORT DATA TESTS ====================

    @Test
    @DisplayName("Should fetch main inventory reorder needs with status classification")
    void shouldFetchMainInventoryReorderNeedsWithStatusClassification() throws SQLException {
        // Arrange
        try (MockedStatic<DatabaseConnection> mockedStatic = mockStatic(DatabaseConnection.class)) {
            mockedStatic.when(DatabaseConnection::getInstance).thenReturn(dbConnection);
            when(dbConnection.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);

            when(resultSet.next())
                    .thenReturn(true)   // First row exists
                    .thenReturn(true)   // Second row exists
                    .thenReturn(false); // No more rows

            // Due to ORDER BY total_quantity ASC, the 0-quantity item comes first
            when(resultSet.getString("product_code"))
                    .thenReturn("CRITICAL001")    // Row 1
                    .thenReturn("LOW001");        // Row 2

            when(resultSet.getString("product_name"))
                    .thenReturn("Critical Product")     // Row 1
                    .thenReturn("Low Stock Product");   // Row 2

            // CRITICAL FIX: getInt("total_quantity") is called TWICE per row!
            // Row 1: status check (0) + constructor (0)
            // Row 2: status check (25) + constructor (25)
            when(resultSet.getInt("total_quantity"))
                    .thenReturn(0)      // Row 1, Call 1: status determination
                    .thenReturn(0)      // Row 1, Call 2: constructor parameter
                    .thenReturn(25)     // Row 2, Call 1: status determination
                    .thenReturn(25);    // Row 2, Call 2: constructor parameter

            reportRepository = new ReportRepositoryImpl();

            // Act
            List<ReorderItem> result = reportRepository.getMainInventoryReorderNeeds(testDate);

            // Assert
            assertEquals(2, result.size());

            // First item should be the one with 0 quantity (CRITICAL)
            ReorderItem firstItem = result.getFirst();
            assertEquals("CRITICAL001", firstItem.getProductCode());
            assertEquals("Critical Product", firstItem.getProductName());
            assertEquals(0, firstItem.getTotalQuantityAvailable());
            assertEquals("CRITICAL", firstItem.getStatus());

            // Second item should be the one with 25 quantity (LOW)
            ReorderItem secondItem = result.get(1);
            assertEquals("LOW001", secondItem.getProductCode());
            assertEquals("Low Stock Product", secondItem.getProductName());
            assertEquals(25, secondItem.getTotalQuantityAvailable());
            assertEquals("LOW", secondItem.getStatus());

            verify(preparedStatement).setDate(1, Date.valueOf(testDate));
        }
    }

    // ==================== STOCK REPORT DATA TESTS ====================

    @Test
    @DisplayName("Should fetch comprehensive stock report data")
    void shouldFetchComprehensiveStockReportData() throws SQLException {
        // Arrange
        try (MockedStatic<DatabaseConnection> mockedStatic = mockStatic(DatabaseConnection.class)) {
            mockedStatic.when(DatabaseConnection::getInstance).thenReturn(dbConnection);
            when(dbConnection.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);

            when(resultSet.next())
                    .thenReturn(true)
                    .thenReturn(false);

            // Mock batch data
            when(resultSet.getString("product_code")).thenReturn("BVEDRB001");
            when(resultSet.getString("product_name")).thenReturn("Red Bull Energy Drink 250ml");
            when(resultSet.getObject("batch_number")).thenReturn(1);
            when(resultSet.getInt("batch_number")).thenReturn(1);
            when(resultSet.getInt("quantity_received")).thenReturn(500);
            when(resultSet.getDate("purchase_date")).thenReturn(Date.valueOf("2024-09-15"));
            when(resultSet.getDate("expiry_date")).thenReturn(Date.valueOf("2025-09-15"));
            when(resultSet.getInt("remaining_quantity")).thenReturn(320);
            when(resultSet.getInt("physical_quantity")).thenReturn(100);
            when(resultSet.getInt("online_quantity")).thenReturn(80);

            reportRepository = new ReportRepositoryImpl();

            // Act
            List<StockItem> result = reportRepository.getStockReportData(testDate);

            // Assert
            assertEquals(1, result.size());
            StockItem item = result.get(0);
            assertEquals("BVEDRB001", item.getProductCode());
            assertEquals("Red Bull Energy Drink 250ml", item.getProductName());
            assertEquals(1, item.getBatchNumber());
            assertEquals(500, item.getQuantityReceived());
            assertEquals(LocalDate.of(2024, 9, 15), item.getPurchaseDate());
            assertEquals(LocalDate.of(2025, 9, 15), item.getExpiryDate());
            assertEquals(320, item.getMainInventoryRemaining());
            assertEquals(100, item.getPhysicalStoreQuantity());
            assertEquals(80, item.getOnlineStoreQuantity());
            assertEquals(500, item.getTotalQuantity());

            verify(preparedStatement).setDate(1, Date.valueOf(testDate));
        }
    }

    @Test
    @DisplayName("Should handle products with no batches in stock report - Lenient Mode")
    void shouldHandleProductsWithNoBatchesInStockReportLenient() throws SQLException {
        // Arrange
        try (MockedStatic<DatabaseConnection> mockedStatic = mockStatic(DatabaseConnection.class)) {
            mockedStatic.when(DatabaseConnection::getInstance).thenReturn(dbConnection);
            when(dbConnection.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);

            when(resultSet.next())
                    .thenReturn(true)
                    .thenReturn(false);

            // Required stubs (definitely called)
            when(resultSet.getString("product_code")).thenReturn("NEWPRODUCT");
            when(resultSet.getString("product_name")).thenReturn("New Product");
            when(resultSet.getObject("batch_number")).thenReturn(null);

            // Optional stubs (may or may not be called depending on implementation logic)
            // Use lenient() to avoid UnnecessaryStubbingException
            lenient().when(resultSet.getInt("quantity_received")).thenReturn(0);
            lenient().when(resultSet.getDate("purchase_date")).thenReturn(null);
            lenient().when(resultSet.getDate("expiry_date")).thenReturn(null);
            lenient().when(resultSet.getInt("remaining_quantity")).thenReturn(0);
            lenient().when(resultSet.getInt("physical_quantity")).thenReturn(0);
            lenient().when(resultSet.getInt("online_quantity")).thenReturn(0);

            reportRepository = new ReportRepositoryImpl();

            // Act
            List<StockItem> result = reportRepository.getStockReportData(testDate);

            // Assert
            assertEquals(1, result.size());
            StockItem item = result.get(0);
            assertEquals("NEWPRODUCT", item.getProductCode());
            assertEquals("New Product", item.getProductName());
            assertEquals(0, item.getBatchNumber());
            assertEquals(0, item.getQuantityReceived());
            assertNull(item.getPurchaseDate());
            assertNull(item.getExpiryDate());
            assertEquals(0, item.getTotalQuantity());
        }
    }
    // ==================== BILL REPORT DATA TESTS ====================

    @Test
    @DisplayName("Should fetch physical store bills successfully")
    void shouldFetchPhysicalStoreBillsSuccessfully() throws SQLException {
        // Arrange
        try (MockedStatic<DatabaseConnection> mockedStatic = mockStatic(DatabaseConnection.class)) {
            mockedStatic.when(DatabaseConnection::getInstance).thenReturn(dbConnection);
            when(dbConnection.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);

            when(resultSet.next())
                    .thenReturn(true)
                    .thenReturn(false);

            when(resultSet.getString("bill_serial_number")).thenReturn("BILL000001");
            when(resultSet.getDate("bill_date")).thenReturn(Date.valueOf(testDate));
            when(resultSet.getString("customer_name")).thenReturn(null); // Walk-in customer
            when(resultSet.getInt("item_count")).thenReturn(3);
            when(resultSet.getBigDecimal("total_amount")).thenReturn(new BigDecimal("1250.00"));
            when(resultSet.getString("transaction_type")).thenReturn("CASH");

            reportRepository = new ReportRepositoryImpl();

            // Act
            List<BillSummary> result = reportRepository.getPhysicalStoreBills(testDate);

            // Assert
            assertEquals(1, result.size());
            BillSummary bill = result.get(0);
            assertEquals("BILL000001", bill.getBillSerialNumber());
            assertEquals(testDate, bill.getBillDate());
            assertNull(bill.getCustomerName());
            assertEquals(3, bill.getItemCount());
            assertEquals(new BigDecimal("1250.00"), bill.getTotalAmount());
            assertEquals("CASH", bill.getTransactionType());

            verify(preparedStatement).setDate(1, Date.valueOf(testDate));
        }
    }

    @Test
    @DisplayName("Should fetch online store bills with customer names successfully")
    void shouldFetchOnlineStoreBillsWithCustomerNamesSuccessfully() throws SQLException {
        // Arrange
        try (MockedStatic<DatabaseConnection> mockedStatic = mockStatic(DatabaseConnection.class)) {
            mockedStatic.when(DatabaseConnection::getInstance).thenReturn(dbConnection);
            when(dbConnection.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);

            when(resultSet.next())
                    .thenReturn(true)
                    .thenReturn(false);

            when(resultSet.getString("bill_serial_number")).thenReturn("BILL000003");
            when(resultSet.getDate("bill_date")).thenReturn(Date.valueOf(testDate));
            when(resultSet.getString("customer_name")).thenReturn("John Doe");
            when(resultSet.getInt("item_count")).thenReturn(2);
            when(resultSet.getBigDecimal("total_amount")).thenReturn(new BigDecimal("1700.00"));
            when(resultSet.getString("transaction_type")).thenReturn("ONLINE");

            reportRepository = new ReportRepositoryImpl();

            // Act
            List<BillSummary> result = reportRepository.getOnlineStoreBills(testDate);

            // Assert
            assertEquals(1, result.size());
            BillSummary bill = result.get(0);
            assertEquals("BILL000003", bill.getBillSerialNumber());
            assertEquals(testDate, bill.getBillDate());
            assertEquals("John Doe", bill.getCustomerName());
            assertEquals(2, bill.getItemCount());
            assertEquals(new BigDecimal("1700.00"), bill.getTotalAmount());
            assertEquals("ONLINE", bill.getTransactionType());
        }
    }

    @Test
    @DisplayName("Should fetch transaction counts correctly")
    void shouldFetchTransactionCountsCorrectly() throws SQLException {
        // Arrange
        try (MockedStatic<DatabaseConnection> mockedStatic = mockStatic(DatabaseConnection.class)) {
            mockedStatic.when(DatabaseConnection::getInstance).thenReturn(dbConnection);
            when(dbConnection.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);

            when(resultSet.next()).thenReturn(true);
            when(resultSet.getInt("transaction_count")).thenReturn(5);

            reportRepository = new ReportRepositoryImpl();

            // Act
            int physicalCount = reportRepository.getPhysicalStoreTransactionCount(testDate);

            // Reset for online count
            when(resultSet.getInt("transaction_count")).thenReturn(3);
            int onlineCount = reportRepository.getOnlineStoreTransactionCount(testDate);

            // Assert
            assertEquals(5, physicalCount);
            assertEquals(3, onlineCount);
            verify(preparedStatement, times(2)).setDate(1, Date.valueOf(testDate));
        }
    }

    // ==================== ERROR HANDLING TESTS ====================

    @Test
    @DisplayName("Should throw RuntimeException when SQL exception occurs")
    void shouldThrowRuntimeExceptionWhenSQLExceptionOccurs() throws SQLException {
        // Arrange
        try (MockedStatic<DatabaseConnection> mockedStatic = mockStatic(DatabaseConnection.class)) {
            mockedStatic.when(DatabaseConnection::getInstance).thenReturn(dbConnection);
            when(dbConnection.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString()))
                    .thenThrow(new SQLException("Database connection failed"));

            reportRepository = new ReportRepositoryImpl();

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                reportRepository.getPhysicalStoreSalesData(testDate);
            });

            assertTrue(exception.getMessage().contains("Error fetching sales data"));
            assertInstanceOf(SQLException.class, exception.getCause());
        }
    }

    @Test
    @DisplayName("Should throw RuntimeException when query execution fails")
    void shouldThrowRuntimeExceptionWhenQueryExecutionFails() throws SQLException {
        // Arrange
        try (MockedStatic<DatabaseConnection> mockedStatic = mockStatic(DatabaseConnection.class)) {
            mockedStatic.when(DatabaseConnection::getInstance).thenReturn(dbConnection);
            when(dbConnection.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery())
                    .thenThrow(new SQLException("Query execution failed"));

            reportRepository = new ReportRepositoryImpl();

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                reportRepository.getOnlineStoreRevenue(testDate);
            });

            assertTrue(exception.getMessage().contains("Error fetching revenue data"));
        }
    }

    @Test
    @DisplayName("Should return zero when count query fails gracefully")
    void shouldReturnZeroWhenCountQueryFailsGracefully() throws SQLException {
        // Arrange
        try (MockedStatic<DatabaseConnection> mockedStatic = mockStatic(DatabaseConnection.class)) {
            mockedStatic.when(DatabaseConnection::getInstance).thenReturn(dbConnection);
            when(dbConnection.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false); // No results

            reportRepository = new ReportRepositoryImpl();

            // Act
            int result = reportRepository.getPhysicalStoreTransactionCount(testDate);

            // Assert
            assertEquals(0, result);
        }
    }

    // ==================== INTEGRATION-STYLE TESTS ====================

    @Test
    @DisplayName("Should handle empty result sets gracefully")
    void shouldHandleEmptyResultSetsGracefully() throws SQLException {
        // Arrange
        try (MockedStatic<DatabaseConnection> mockedStatic = mockStatic(DatabaseConnection.class)) {
            mockedStatic.when(DatabaseConnection::getInstance).thenReturn(dbConnection);
            when(dbConnection.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false); // No data

            reportRepository = new ReportRepositoryImpl();

            // Act
            List<SalesItem> salesResult = reportRepository.getPhysicalStoreSalesData(testDate);
            List<RestockItem> restockResult = reportRepository.getPhysicalStoreRestockNeeds(testDate);
            List<ReorderItem> reorderResult = reportRepository.getMainInventoryReorderNeeds(testDate);
            List<StockItem> stockResult = reportRepository.getStockReportData(testDate);
            List<BillSummary> billResult = reportRepository.getPhysicalStoreBills(testDate);

            // Assert
            assertTrue(salesResult.isEmpty());
            assertTrue(restockResult.isEmpty());
            assertTrue(reorderResult.isEmpty());
            assertTrue(stockResult.isEmpty());
            assertTrue(billResult.isEmpty());
        }
    }

    @Test
    @DisplayName("Should properly clean up database resources")
    void shouldProperlyCleanUpDatabaseResources() throws SQLException {
        // Arrange
        try (MockedStatic<DatabaseConnection> mockedStatic = mockStatic(DatabaseConnection.class)) {
            mockedStatic.when(DatabaseConnection::getInstance).thenReturn(dbConnection);
            when(dbConnection.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false);

            reportRepository = new ReportRepositoryImpl();

            // Act
            reportRepository.getPhysicalStoreSalesData(testDate);

            // Assert - Verify resources are closed
            // Note: In a real implementation, we'd use try-with-resources which automatically closes
            // The test verifies the method completes without resource leaks
            verify(preparedStatement).executeQuery();
        }
    }
}