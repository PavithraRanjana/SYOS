package com.syos.repository.impl;

import com.syos.domain.models.Bill;
import com.syos.domain.models.BillItem;
import com.syos.domain.valueobjects.BillSerialNumber;
import com.syos.domain.valueobjects.ProductCode;
import com.syos.domain.valueobjects.Money;
import com.syos.domain.enums.TransactionType;
import com.syos.domain.enums.StoreType;
import com.syos.utils.DatabaseConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillRepositoryImplTest {

    private Connection connection;
    private PreparedStatement preparedStatement;
    private PreparedStatement itemPreparedStatement;
    private ResultSet resultSet;
    private Statement statement;

    private BillRepositoryImpl billRepository;
    private BillSerialNumber testSerialNumber;
    private ProductCode testProductCode;
    private Money testMoney;

    @BeforeEach
    void setUp() throws SQLException {
        testSerialNumber = new BillSerialNumber("BILL000001");
        testProductCode = new ProductCode("TEST001");
        testMoney = new Money(new BigDecimal("10.50"));

        // Mock the database connection and related objects
        connection = mock(Connection.class);
        preparedStatement = mock(PreparedStatement.class);
        itemPreparedStatement = mock(PreparedStatement.class);
        resultSet = mock(ResultSet.class);
        statement = mock(Statement.class);

        // Mock the DatabaseConnection singleton
        try (MockedStatic<DatabaseConnection> mockedDatabaseConnection = mockStatic(DatabaseConnection.class)) {
            DatabaseConnection databaseConnectionInstance = mock(DatabaseConnection.class);
            when(databaseConnectionInstance.getConnection()).thenReturn(connection);
            mockedDatabaseConnection.when(DatabaseConnection::getInstance).thenReturn(databaseConnectionInstance);

            // Create the repository instance
            billRepository = new BillRepositoryImpl();
        }
    }

    @Test
    @DisplayName("Should generate next serial number successfully")
    void shouldGenerateNextSerialNumberSuccessfully() throws SQLException {
        // Given
        LocalDate billDate = LocalDate.now();
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("next_number")).thenReturn(2);

        // When
        BillSerialNumber result = billRepository.generateNextSerialNumber(billDate);

        // Then
        assertNotNull(result);
        assertEquals("BILL000002", result.getSerialNumber());
        verify(preparedStatement).executeQuery();
    }

    @Test
    @DisplayName("Should generate first serial number when no bills exist")
    void shouldGenerateFirstSerialNumberWhenNoBillsExist() throws SQLException {
        // Given
        LocalDate billDate = LocalDate.now();
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false); // No existing bills

        // When
        BillSerialNumber result = billRepository.generateNextSerialNumber(billDate);

        // Then
        assertNotNull(result);
        assertEquals("BILL000001", result.getSerialNumber());
    }

    @Test
    @DisplayName("Should handle SQLException when generating serial number")
    void shouldHandleSQLExceptionWhenGeneratingSerialNumber() throws SQLException {
        // Given
        LocalDate billDate = LocalDate.now();
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("Database error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> billRepository.generateNextSerialNumber(billDate));
        assertTrue(exception.getMessage().contains("Error generating bill serial number"));
    }



    @Test
    @DisplayName("Should save bill with null customer ID successfully")
    void shouldSaveBillWithNullCustomerIdSuccessfully() throws SQLException {
        // Given
        Bill bill = createTestBillWithNullCustomer();
        when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(preparedStatement);
        when(connection.prepareStatement(anyString())).thenReturn(itemPreparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(preparedStatement.getGeneratedKeys()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(100);

        // When
        Bill result = billRepository.saveBillWithItems(bill);

        // Then
        assertNotNull(result);
        assertNull(result.getCustomerId());
        verify(preparedStatement).setObject(2, null);
    }

    @Test
    @DisplayName("Should save bill with null cash payment details successfully")
    void shouldSaveBillWithNullCashPaymentDetailsSuccessfully() throws SQLException {
        // Given
        Bill bill = createTestBillWithoutCashPayment();
        when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(preparedStatement);
        when(connection.prepareStatement(anyString())).thenReturn(itemPreparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(preparedStatement.getGeneratedKeys()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(100);

        // When
        Bill result = billRepository.saveBillWithItems(bill);

        // Then
        assertNotNull(result);
        assertNull(result.getCashTendered());
        assertNull(result.getChangeAmount());
        verify(preparedStatement).setBigDecimal(8, null);
        verify(preparedStatement).setBigDecimal(9, null);
    }

    @Test
    @DisplayName("Should handle SQLException when saving bill")
    void shouldHandleSQLExceptionWhenSavingBill() throws SQLException {
        // Given
        Bill bill = createTestBill();
        when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenThrow(new SQLException("Database error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> billRepository.saveBillWithItems(bill));
        assertTrue(exception.getMessage().contains("Error saving bill with items"));
        verify(connection).rollback();
    }



    @Test
    @DisplayName("Should handle SQLException when resetting auto-commit")
    void shouldHandleSQLExceptionWhenResettingAutoCommit() throws SQLException {
        // Given
        Bill bill = createTestBill();
        when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(preparedStatement);
        when(connection.prepareStatement(anyString())).thenReturn(itemPreparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(preparedStatement.getGeneratedKeys()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(100);
        doThrow(new SQLException("Auto-commit reset failed")).when(connection).setAutoCommit(true);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> billRepository.saveBillWithItems(bill));
        assertTrue(exception.getMessage().contains("Error resetting auto-commit"));
    }


    @Test
    @DisplayName("Should return empty list when no bills found for customer")
    void shouldReturnEmptyListWhenNoBillsFoundForCustomer() throws SQLException {
        // Given
        Integer customerId = 999;
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false); // No bills found

        // When
        List<Bill> result = billRepository.findByCustomerId(customerId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(preparedStatement).setInt(1, 999);
    }

    @Test
    @DisplayName("Should handle SQLException when finding bills by customer ID")
    void shouldHandleSQLExceptionWhenFindingBillsByCustomerId() throws SQLException {
        // Given
        Integer customerId = 123;
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("Database error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> billRepository.findByCustomerId(customerId));
        assertTrue(exception.getMessage().contains("Error finding bills by customer ID"));
    }

    @Test
    @DisplayName("Should handle bills with no items for customer")
    void shouldHandleBillsWithNoItemsForCustomer() throws SQLException {
        // Given
        Integer customerId = 123;
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        // Mock bill result with no items
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getInt("bill_id")).thenReturn(100);
        when(resultSet.getString("bill_serial_number")).thenReturn("BILL000001");
        when(resultSet.getInt("customer_id")).thenReturn(123);
        when(resultSet.getString("transaction_type")).thenReturn("ONLINE");
        when(resultSet.getString("store_type")).thenReturn("ONLINE");
        when(resultSet.getBigDecimal("discount_amount")).thenReturn(new BigDecimal("1.00"));
        when(resultSet.getDate("bill_date")).thenReturn(Date.valueOf(LocalDate.now()));

        // Mock empty item results
        ResultSet itemResultSet = mock(ResultSet.class);
        when(preparedStatement.executeQuery()).thenReturn(resultSet, itemResultSet);
        when(itemResultSet.next()).thenReturn(false); // No items

        // When
        List<Bill> result = billRepository.findByCustomerId(customerId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getItems().isEmpty());
    }

    @Test
    @DisplayName("Should throw UnsupportedOperationException for findById")
    void shouldThrowUnsupportedOperationExceptionForFindById() {
        // When & Then
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
                () -> billRepository.findById(1));
        assertEquals("Not implemented yet", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw UnsupportedOperationException for findByDate")
    void shouldThrowUnsupportedOperationExceptionForFindByDate() {
        // When & Then
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
                () -> billRepository.findByDate(LocalDate.now()));
        assertEquals("Not implemented yet", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw UnsupportedOperationException for findByDateRange")
    void shouldThrowUnsupportedOperationExceptionForFindByDateRange() {
        // When & Then
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
                () -> billRepository.findByDateRange(LocalDate.now(), LocalDate.now()));
        assertEquals("Not implemented yet", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw UnsupportedOperationException for findAll")
    void shouldThrowUnsupportedOperationExceptionForFindAll() {
        // When & Then
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
                () -> billRepository.findAll());
        assertEquals("Not implemented yet", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw UnsupportedOperationException for delete")
    void shouldThrowUnsupportedOperationExceptionForDelete() {
        // When & Then
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
                () -> billRepository.delete(1));
        assertEquals("Not implemented yet", exception.getMessage());
    }

    @Test
    @DisplayName("Should call saveBillWithItems when save is called")
    void shouldCallSaveBillWithItemsWhenSaveIsCalled() throws SQLException {
        // Given
        Bill bill = createTestBill();
        when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(preparedStatement);
        when(connection.prepareStatement(anyString())).thenReturn(itemPreparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(preparedStatement.getGeneratedKeys()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(100);

        // When
        Bill result = billRepository.save(bill);

        // Then
        assertNotNull(result);
        assertEquals(bill, result);
        // The save method should delegate to saveBillWithItems
    }


    @Test
    @DisplayName("Should handle bill with zero discount amount")
    void shouldHandleBillWithZeroDiscountAmount() throws SQLException {
        // Given
        Bill bill = createTestBillWithZeroDiscount();
        when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(preparedStatement);
        when(connection.prepareStatement(anyString())).thenReturn(itemPreparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(preparedStatement.getGeneratedKeys()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(100);

        // When
        Bill result = billRepository.saveBillWithItems(bill);

        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("0.00"), result.getDiscountAmount().getAmount());
    }

    // Helper method to create a test bill
    private Bill createTestBill() {
        Bill bill = new Bill(
                testSerialNumber,
                123,
                TransactionType.CASH,
                StoreType.PHYSICAL,
                new Money(new BigDecimal("1.00")),
                LocalDate.now()
        );

        BillItem item1 = new BillItem(
                testProductCode,
                "Test Product 1",
                2,
                testMoney,
                1000
        );

        BillItem item2 = new BillItem(
                new ProductCode("TEST002"),
                "Test Product 2",
                1,
                new Money(new BigDecimal("5.00")),
                1001
        );

        bill.addItem(item1);
        bill.addItem(item2);

        // Process cash payment
        bill.processCashPayment(new Money(new BigDecimal("25.00")));

        return bill;
    }

    // Helper method to create a test bill with null customer ID
    private Bill createTestBillWithNullCustomer() {
        Bill bill = new Bill(
                testSerialNumber,
                null, // Null customer ID for walk-in customer
                TransactionType.CASH,
                StoreType.PHYSICAL,
                new Money(new BigDecimal("1.00")),
                LocalDate.now()
        );

        BillItem item = new BillItem(
                testProductCode,
                "Test Product",
                1,
                testMoney,
                1000
        );

        bill.addItem(item);
        bill.processCashPayment(new Money(new BigDecimal("15.00")));

        return bill;
    }

    // Helper method to create a test bill without cash payment
    private Bill createTestBillWithoutCashPayment() {
        Bill bill = new Bill(
                testSerialNumber,
                123,
                TransactionType.ONLINE, // Online transaction, no cash payment
                StoreType.ONLINE,
                new Money(new BigDecimal("1.00")),
                LocalDate.now()
        );

        BillItem item = new BillItem(
                testProductCode,
                "Test Product",
                1,
                testMoney,
                1000
        );

        bill.addItem(item);
        // Don't process cash payment for online transactions

        return bill;
    }

    // Helper method to create a test bill with multiple items
    private Bill createTestBillWithMultipleItems() {
        Bill bill = new Bill(
                testSerialNumber,
                123,
                TransactionType.CASH,
                StoreType.PHYSICAL,
                new Money(new BigDecimal("2.00")),
                LocalDate.now()
        );

        // Add multiple items
        for (int i = 1; i <= 3; i++) {
            BillItem item = new BillItem(
                    new ProductCode("TEST" + i),
                    "Test Product " + i,
                    i,
                    new Money(new BigDecimal(i * 5.00)),
                    1000 + i
            );
            bill.addItem(item);
        }

        bill.processCashPayment(new Money(new BigDecimal("50.00")));

        return bill;
    }

    // Helper method to create a test bill with zero discount
    private Bill createTestBillWithZeroDiscount() {
        Bill bill = new Bill(
                testSerialNumber,
                123,
                TransactionType.CASH,
                StoreType.PHYSICAL,
                new Money(new BigDecimal("0.00")), // Zero discount
                LocalDate.now()
        );

        BillItem item = new BillItem(
                testProductCode,
                "Test Product",
                1,
                testMoney,
                1000
        );

        bill.addItem(item);
        bill.processCashPayment(new Money(new BigDecimal("15.00")));

        return bill;
    }
}