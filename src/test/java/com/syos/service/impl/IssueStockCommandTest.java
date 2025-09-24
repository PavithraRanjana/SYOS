package com.syos.service.impl;

import com.syos.domain.models.MainInventory;
import com.syos.domain.valueobjects.ProductCode;
import com.syos.domain.valueobjects.Money;
import com.syos.domain.enums.StoreType;
import com.syos.exceptions.InventoryException;
import com.syos.repository.interfaces.InventoryRepository;
import com.syos.service.interfaces.InventoryCommand.CommandResult;
import com.syos.service.interfaces.InventoryCommand.CommandType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueStockCommandTest {

    @Mock
    private InventoryRepository inventoryRepository;

    private IssueStockCommand issueStockCommand;
    private ProductCode productCode;
    private MainInventory sourceBatch;
    private final int quantity = 50;
    private final StoreType targetStore = StoreType.PHYSICAL;

    @BeforeEach
    void setUp() {
        productCode = new ProductCode("TEST001");

        sourceBatch = new MainInventory(
                123, // batchNumber
                productCode,
                100, // quantityReceived
                new Money(BigDecimal.valueOf(25.00)), // purchasePrice
                LocalDate.now().minusDays(10), // purchaseDate
                LocalDate.now().plusMonths(6), // expiryDate
                "Test Supplier",
                80   // remainingQuantity
        );

        issueStockCommand = new IssueStockCommand(
                inventoryRepository,
                productCode,
                quantity,
                targetStore,
                sourceBatch
        );
    }

    @Test
    @DisplayName("Should execute successfully when issuing to physical store")
    void shouldExecuteSuccessfullyToPhysicalStore() throws InventoryException {
        // When
        CommandResult result = issueStockCommand.execute();

        // Then
        verify(inventoryRepository).issueToPhysicalStore(productCode, sourceBatch.getBatchNumber(), quantity);
        verify(inventoryRepository).reduceMainInventoryStock(sourceBatch.getBatchNumber(), quantity);
        assertTrue(issueStockCommand.canUndo());

        assertNotNull(result.getMessage());
        assertTrue(result.getMessage().contains("TEST001"));
        assertTrue(result.getMessage().contains("50 units"));
    }

    @Test
    @DisplayName("Should execute successfully when issuing to online store")
    void shouldExecuteSuccessfullyToOnlineStore() throws InventoryException {
        // Given
        IssueStockCommand onlineCommand = new IssueStockCommand(
                inventoryRepository, productCode, quantity, StoreType.ONLINE, sourceBatch
        );

        // When
        CommandResult result = onlineCommand.execute();

        // Then
        verify(inventoryRepository).issueToOnlineStore(productCode, sourceBatch.getBatchNumber(), quantity);
        verify(inventoryRepository).reduceMainInventoryStock(sourceBatch.getBatchNumber(), quantity);
        assertTrue(onlineCommand.canUndo());
    }

    @Test
    @DisplayName("Should issue partial quantity when insufficient stock")
    void shouldIssuePartialQuantityWhenInsufficientStock() throws InventoryException {
        // Given - Batch has only 30 units remaining (more than 0)
        MainInventory lowStockBatch = new MainInventory(
                123, productCode, 100, new Money(BigDecimal.valueOf(25.00)),
                LocalDate.now(), LocalDate.now().plusMonths(6), "Supplier", 30
        );

        IssueStockCommand partialCommand = new IssueStockCommand(
                inventoryRepository, productCode, 50, targetStore, lowStockBatch
        );

        // When
        CommandResult result = partialCommand.execute();

        // Then
        assertNotNull(result.getMessage());
        assertTrue(result.getMessage().contains("Quantity Issued: 30 units (requested 50)"));

        verify(inventoryRepository).issueToPhysicalStore(productCode, lowStockBatch.getBatchNumber(), 30);
        verify(inventoryRepository).reduceMainInventoryStock(lowStockBatch.getBatchNumber(), 30);
    }

    @Test
    @DisplayName("Should throw exception when batch has no remaining stock")
    void shouldThrowExceptionWhenNoStockAvailable() {
        // Given - Batch has zero remaining quantity
        MainInventory emptyBatch = new MainInventory(
                123, productCode, 100, new Money(BigDecimal.valueOf(25.00)),
                LocalDate.now(), LocalDate.now().plusMonths(6), "Supplier", 0
        );

        IssueStockCommand noStockCommand = new IssueStockCommand(
                inventoryRepository, productCode, quantity, targetStore, emptyBatch
        );

        // When & Then
        InventoryException exception = assertThrows(InventoryException.class,
                () -> noStockCommand.execute());

        assertTrue(exception.getMessage().contains("Selected batch has no remaining stock"));
        verify(inventoryRepository, never()).issueToPhysicalStore(any(), anyInt(), anyInt());
        verify(inventoryRepository, never()).reduceMainInventoryStock(anyInt(), anyInt());
        assertFalse(noStockCommand.canUndo());
    }

    @Test
    @DisplayName("Should throw exception when quantity is zero or negative")
    void shouldThrowExceptionWhenQuantityInvalid() {
        // Given
        IssueStockCommand zeroQuantityCommand = new IssueStockCommand(
                inventoryRepository, productCode, 0, targetStore, sourceBatch
        );

        IssueStockCommand negativeQuantityCommand = new IssueStockCommand(
                inventoryRepository, productCode, -5, targetStore, sourceBatch
        );

        // When & Then
        InventoryException exception1 = assertThrows(InventoryException.class,
                () -> zeroQuantityCommand.execute());
        assertTrue(exception1.getMessage().contains("Issue quantity must be positive"));

        InventoryException exception2 = assertThrows(InventoryException.class,
                () -> negativeQuantityCommand.execute());
        assertTrue(exception2.getMessage().contains("Issue quantity must be positive"));
    }

    @Test
    @DisplayName("Should throw exception when batch is expired")
    void shouldThrowExceptionWhenBatchExpired() {
        // Given - Batch expired yesterday
        MainInventory expiredBatch = new MainInventory(
                123, productCode, 100, new Money(BigDecimal.valueOf(25.00)),
                LocalDate.now().minusMonths(1), LocalDate.now().minusDays(1), "Supplier", 50
        );

        IssueStockCommand expiredCommand = new IssueStockCommand(
                inventoryRepository, productCode, quantity, targetStore, expiredBatch
        );

        // When & Then
        InventoryException exception = assertThrows(InventoryException.class,
                () -> expiredCommand.execute());
        assertTrue(exception.getMessage().contains("expired batch"));
    }

    @Test
    @DisplayName("Should throw exception when product codes don't match")
    void shouldThrowExceptionWhenProductCodesDontMatch() {
        // Given - Different product code
        ProductCode differentProductCode = new ProductCode("DIFF001");
        IssueStockCommand mismatchCommand = new IssueStockCommand(
                inventoryRepository, differentProductCode, quantity, targetStore, sourceBatch
        );

        // When & Then
        InventoryException exception = assertThrows(InventoryException.class,
                () -> mismatchCommand.execute());
        assertTrue(exception.getMessage().contains("Batch product code does not match request"));
    }

    @Test
    @DisplayName("Should perform undo operations after execution")
    void shouldPerformUndoOperationsAfterExecution() throws InventoryException {
        // Given
        issueStockCommand.execute();
        assertTrue(issueStockCommand.canUndo());

        // When
        CommandResult result = issueStockCommand.undo();

        // Then
        verify(inventoryRepository).returnFromPhysicalStore(productCode, sourceBatch.getBatchNumber(), quantity);
        verify(inventoryRepository).restoreMainInventoryStock(sourceBatch.getBatchNumber(), quantity);
        assertFalse(issueStockCommand.canUndo());
        assertNotNull(result.getMessage());
    }

    @Test
    @DisplayName("Should perform undo operations for online store")
    void shouldPerformUndoOperationsForOnlineStore() throws InventoryException {
        // Given
        IssueStockCommand onlineCommand = new IssueStockCommand(
                inventoryRepository, productCode, quantity, StoreType.ONLINE, sourceBatch
        );
        onlineCommand.execute();

        // When
        onlineCommand.undo();

        // Then
        verify(inventoryRepository).returnFromOnlineStore(productCode, sourceBatch.getBatchNumber(), quantity);
        verify(inventoryRepository).restoreMainInventoryStock(sourceBatch.getBatchNumber(), quantity);
        assertFalse(onlineCommand.canUndo());
    }

    @Test
    @DisplayName("Should handle undo without execution")
    void shouldHandleUndoWithoutExecution() throws InventoryException {
        // When
        CommandResult result = issueStockCommand.undo();

        // Then
        assertNotNull(result.getMessage());
        assertTrue(result.getMessage().contains("Cannot undo") || result.getMessage().contains("No stock was issued"));
        verify(inventoryRepository, never()).returnFromPhysicalStore(any(), anyInt(), anyInt());
        verify(inventoryRepository, never()).restoreMainInventoryStock(anyInt(), anyInt());
    }

    @Test
    @DisplayName("Should throw exception when undo operation fails")
    void shouldThrowExceptionWhenUndoOperationFails() throws InventoryException {
        // Given
        issueStockCommand.execute();

        // Setup undo to fail
        doThrow(new RuntimeException("Database error")).when(inventoryRepository)
                .returnFromPhysicalStore(any(), anyInt(), anyInt());

        // When & Then
        InventoryException exception = assertThrows(InventoryException.class,
                () -> issueStockCommand.undo());
        assertTrue(exception.getMessage().contains("Failed to undo stock issue"));
    }

    @Test
    @DisplayName("Should return correct description")
    void shouldReturnCorrectDescription() {
        // When
        String description = issueStockCommand.getDescription();

        // Then
        assertEquals("Issue 50 units of TEST001 to physical store from batch #123", description);
    }

    @Test
    @DisplayName("Should return correct command type")
    void shouldReturnCorrectCommandType() {
        // When
        CommandType commandType = issueStockCommand.getCommandType();

        // Then
        assertEquals(CommandType.ISSUE_STOCK, commandType);
    }

    @Test
    @DisplayName("Should handle batch with null expiry date")
    void shouldHandleBatchWithNullExpiryDate() throws InventoryException {
        // Given - Batch with null expiry date
        MainInventory noExpiryBatch = new MainInventory(
                123, productCode, 100, new Money(BigDecimal.valueOf(25.00)),
                LocalDate.now(), null, "Supplier", 50
        );

        IssueStockCommand noExpiryCommand = new IssueStockCommand(
                inventoryRepository, productCode, 20, targetStore, noExpiryBatch
        );

        // When
        CommandResult result = noExpiryCommand.execute();

        // Then
        assertNotNull(result.getMessage());
        verify(inventoryRepository).issueToPhysicalStore(productCode, noExpiryBatch.getBatchNumber(), 20);
    }

    @Test
    @DisplayName("Should include expiry warning when batch expires soon")
    void shouldIncludeExpiryWarningWhenBatchExpiresSoon() throws InventoryException {
        // Given - Batch expires in 15 days
        MainInventory soonExpiringBatch = new MainInventory(
                123, productCode, 100, new Money(BigDecimal.valueOf(25.00)),
                LocalDate.now(), LocalDate.now().plusDays(15), "Supplier", 50
        );

        IssueStockCommand soonExpiringCommand = new IssueStockCommand(
                inventoryRepository, productCode, 20, targetStore, soonExpiringBatch
        );

        // When
        CommandResult result = soonExpiringCommand.execute();

        // Then
        assertNotNull(result.getMessage());
        assertTrue(result.getMessage().contains("⚠️") || result.getMessage().contains("expires"));
    }

    @Test
    @DisplayName("Should handle repository exceptions during execution")
    void shouldHandleRepositoryExceptionsDuringExecution() {
        // Given - Repository throws exception
        doThrow(new RuntimeException("Issue failed")).when(inventoryRepository)
                .issueToPhysicalStore(any(), anyInt(), anyInt());

        // When & Then
        InventoryException exception = assertThrows(InventoryException.class,
                () -> issueStockCommand.execute());
        assertTrue(exception.getMessage().contains("Failed to issue stock"));
        assertFalse(issueStockCommand.canUndo());
    }

    @Test
    @DisplayName("IssueResult should contain correct data")
    void issueResultShouldContainCorrectData() {
        // Given
        IssueStockCommand.IssueResult issueResult = new IssueStockCommand.IssueResult(
                productCode, quantity, targetStore, sourceBatch.getBatchNumber()
        );

        // When & Then
        assertEquals(productCode, issueResult.getProductCode());
        assertEquals(quantity, issueResult.getQuantityIssued());
        assertEquals(targetStore, issueResult.getTargetStore());
        assertEquals(sourceBatch.getBatchNumber(), issueResult.getBatchNumber());
    }
}