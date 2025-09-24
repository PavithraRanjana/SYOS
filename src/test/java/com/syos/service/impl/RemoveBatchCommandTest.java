package com.syos.service.impl;

import com.syos.domain.models.MainInventory;
import com.syos.domain.valueobjects.ProductCode;
import com.syos.domain.valueobjects.Money;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RemoveBatchCommandTest {

    @Mock
    private InventoryRepository inventoryRepository;

    private RemoveBatchCommand removeBatchCommand;
    private final int batchNumber = 123;
    private MainInventory mockBatch;

    @BeforeEach
    void setUp() {
        removeBatchCommand = new RemoveBatchCommand(inventoryRepository, batchNumber);

        // Create a mock batch for testing using the constructor
        mockBatch = new MainInventory(
                batchNumber,
                new ProductCode("TEST001"),
                100, // quantityReceived
                new Money(BigDecimal.valueOf(50.00)), // purchasePrice
                LocalDate.now(), // purchaseDate
                LocalDate.now().plusMonths(6), // expiryDate
                "Test Supplier",
                100 // remainingQuantity
        );
    }

    @Test
    @DisplayName("Should execute successfully when batch can be removed")
    void shouldExecuteSuccessfully() throws InventoryException {
        // Given
        when(inventoryRepository.findBatchByNumber(batchNumber))
                .thenReturn(Optional.of(mockBatch));
        when(inventoryRepository.getPhysicalStoreUsage(batchNumber)).thenReturn(0);
        when(inventoryRepository.getOnlineStoreUsage(batchNumber)).thenReturn(0);
        when(inventoryRepository.batchHasBeenSold(batchNumber)).thenReturn(false);
        // removeBatch returns void, so no need to mock return value

        // When
        CommandResult result = removeBatchCommand.execute();

        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.getMessage());
        assertTrue(result.getMessage().contains("✅ Successfully removed batch"));
        assertTrue(result.getMessage().contains("#" + batchNumber));

        verify(inventoryRepository).findBatchByNumber(batchNumber);
        verify(inventoryRepository).getPhysicalStoreUsage(batchNumber);
        verify(inventoryRepository).getOnlineStoreUsage(batchNumber);
        verify(inventoryRepository).batchHasBeenSold(batchNumber);
        verify(inventoryRepository).removeBatch(batchNumber);
    }

    @Test
    @DisplayName("Should throw exception when batch not found")
    void shouldThrowExceptionWhenBatchNotFound() {
        // Given
        when(inventoryRepository.findBatchByNumber(batchNumber))
                .thenReturn(Optional.empty());

        // When & Then
        InventoryException exception = assertThrows(InventoryException.class,
                () -> removeBatchCommand.execute());

        assertTrue(exception.getMessage().contains("Batch not found"));
        verify(inventoryRepository, never()).removeBatch(anyInt());
    }

    @Test
    @DisplayName("Should throw exception when batch has physical store usage")
    void shouldThrowExceptionWhenBatchHasPhysicalStoreUsage() {
        // Given
        when(inventoryRepository.findBatchByNumber(batchNumber))
                .thenReturn(Optional.of(mockBatch));
        when(inventoryRepository.getPhysicalStoreUsage(batchNumber)).thenReturn(50);

        // When & Then
        InventoryException exception = assertThrows(InventoryException.class,
                () -> removeBatchCommand.execute());

        assertTrue(exception.getMessage().contains("units are in physical store inventory"));
        verify(inventoryRepository, never()).removeBatch(anyInt());
    }

    @Test
    @DisplayName("Should throw exception when batch has online store usage")
    void shouldThrowExceptionWhenBatchHasOnlineStoreUsage() {
        // Given
        when(inventoryRepository.findBatchByNumber(batchNumber))
                .thenReturn(Optional.of(mockBatch));
        when(inventoryRepository.getPhysicalStoreUsage(batchNumber)).thenReturn(0);
        when(inventoryRepository.getOnlineStoreUsage(batchNumber)).thenReturn(30);

        // When & Then
        InventoryException exception = assertThrows(InventoryException.class,
                () -> removeBatchCommand.execute());

        assertTrue(exception.getMessage().contains("units are in online store inventory"));
        verify(inventoryRepository, never()).removeBatch(anyInt());
    }

    @Test
    @DisplayName("Should throw exception when batch has been sold")
    void shouldThrowExceptionWhenBatchHasBeenSold() {
        // Given
        when(inventoryRepository.findBatchByNumber(batchNumber))
                .thenReturn(Optional.of(mockBatch));
        when(inventoryRepository.getPhysicalStoreUsage(batchNumber)).thenReturn(0);
        when(inventoryRepository.getOnlineStoreUsage(batchNumber)).thenReturn(0);
        when(inventoryRepository.batchHasBeenSold(batchNumber)).thenReturn(true);

        // When & Then
        InventoryException exception = assertThrows(InventoryException.class,
                () -> removeBatchCommand.execute());

        assertTrue(exception.getMessage().contains("historical sales transactions"));
        verify(inventoryRepository, never()).removeBatch(anyInt());
    }

    @Test
    @DisplayName("Should throw exception when repository remove fails")
    void shouldThrowExceptionWhenRemoveFails() {
        // Given
        when(inventoryRepository.findBatchByNumber(batchNumber))
                .thenReturn(Optional.of(mockBatch));
        when(inventoryRepository.getPhysicalStoreUsage(batchNumber)).thenReturn(0);
        when(inventoryRepository.getOnlineStoreUsage(batchNumber)).thenReturn(0);
        when(inventoryRepository.batchHasBeenSold(batchNumber)).thenReturn(false);
        doThrow(new RuntimeException("Database error")).when(inventoryRepository).removeBatch(batchNumber);

        // When & Then
        InventoryException exception = assertThrows(InventoryException.class,
                () -> removeBatchCommand.execute());

        assertTrue(exception.getMessage().contains("Failed to remove batch"));
        assertTrue(exception.getCause().getMessage().contains("Database error"));
    }

    @Test
    @DisplayName("Should undo successfully when batch was removed")
    void shouldUndoSuccessfully() throws InventoryException {
        // Given - First execute the command
        when(inventoryRepository.findBatchByNumber(batchNumber))
                .thenReturn(Optional.of(mockBatch));
        when(inventoryRepository.getPhysicalStoreUsage(batchNumber)).thenReturn(0);
        when(inventoryRepository.getOnlineStoreUsage(batchNumber)).thenReturn(0);
        when(inventoryRepository.batchHasBeenSold(batchNumber)).thenReturn(false);

        // Execute the command first
        CommandResult executeResult = removeBatchCommand.execute();
        assertTrue(executeResult.isSuccess(), "Execute should be successful first");

        // Verify that we can undo after execution
        assertTrue(removeBatchCommand.canUndo(), "Should be able to undo after execution");

        // Setup undo
        MainInventory restoredBatch = new MainInventory(
                batchNumber,
                mockBatch.getProductCode(),
                mockBatch.getQuantityReceived(),
                mockBatch.getPurchasePrice(),
                mockBatch.getPurchaseDate(),
                mockBatch.getExpiryDate(),
                mockBatch.getSupplierName(),
                mockBatch.getRemainingQuantity()
        );

        when(inventoryRepository.restoreBatch(mockBatch)).thenReturn(restoredBatch);

        // When
        CommandResult result = removeBatchCommand.undo();

        // Then
        assertTrue(result.isSuccess(),
                "Undo should be successful. Actual success: " + result.isSuccess() +
                        ", Message: " + result.getMessage());

        // Check if the message contains any indication of success
        assertTrue(result.getMessage() != null && !result.getMessage().isEmpty(),
                "Message should not be null or empty");

        verify(inventoryRepository).restoreBatch(mockBatch);
    }

    @Test
    @DisplayName("Should return failure when undo without previous execution")
    void shouldReturnFailureWhenUndoWithoutExecution() throws InventoryException {
        // When
        CommandResult result = removeBatchCommand.undo();

        // Then
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Cannot undo: No batch was removed"));
        verify(inventoryRepository, never()).restoreBatch(any());
    }

    @Test
    @DisplayName("Should throw exception when undo restore fails")
    void shouldThrowExceptionWhenUndoRestoreFails() throws InventoryException {
        // Given - First execute the command
        when(inventoryRepository.findBatchByNumber(batchNumber))
                .thenReturn(Optional.of(mockBatch));
        when(inventoryRepository.getPhysicalStoreUsage(batchNumber)).thenReturn(0);
        when(inventoryRepository.getOnlineStoreUsage(batchNumber)).thenReturn(0);
        when(inventoryRepository.batchHasBeenSold(batchNumber)).thenReturn(false);
        // removeBatch returns void, so no need to mock return value

        removeBatchCommand.execute();

        // Setup undo to fail
        when(inventoryRepository.restoreBatch(mockBatch))
                .thenThrow(new RuntimeException("Restore failed"));

        // When & Then
        InventoryException exception = assertThrows(InventoryException.class,
                () -> removeBatchCommand.undo());

        assertTrue(exception.getMessage().contains("Failed to undo batch removal"));
        assertTrue(exception.getCause().getMessage().contains("Restore failed"));
    }

    @Test
    @DisplayName("Should return correct canUndo status")
    void shouldReturnCorrectCanUndoStatus() throws InventoryException {
        // Initially should be false
        assertFalse(removeBatchCommand.canUndo());

        // After execution, should be true
        when(inventoryRepository.findBatchByNumber(batchNumber))
                .thenReturn(Optional.of(mockBatch));
        when(inventoryRepository.getPhysicalStoreUsage(batchNumber)).thenReturn(0);
        when(inventoryRepository.getOnlineStoreUsage(batchNumber)).thenReturn(0);
        when(inventoryRepository.batchHasBeenSold(batchNumber)).thenReturn(false);
        // removeBatch returns void, so no need to mock return value

        removeBatchCommand.execute();
        assertTrue(removeBatchCommand.canUndo());

        // After undo, should be false again
        when(inventoryRepository.restoreBatch(mockBatch)).thenReturn(mockBatch);
        removeBatchCommand.undo();
        assertFalse(removeBatchCommand.canUndo());
    }

    @Test
    @DisplayName("Should return correct description")
    void shouldReturnCorrectDescription() {
        // When
        String description = removeBatchCommand.getDescription();

        // Then
        assertEquals("Remove batch #" + batchNumber, description);
    }

    @Test
    @DisplayName("Should return correct command type")
    void shouldReturnCorrectCommandType() {
        // When
        CommandType commandType = removeBatchCommand.getCommandType();

        // Then
        assertEquals(CommandType.REMOVE_BATCH, commandType);
    }

    @Test
    @DisplayName("Should handle null supplier name gracefully")
    void shouldHandleNullSupplierName() throws InventoryException {
        // Given - Create batch with null supplier
        MainInventory batchWithNullSupplier = new MainInventory(
                batchNumber,
                new ProductCode("TEST001"),
                100,
                new Money(BigDecimal.valueOf(50.00)),
                LocalDate.now(),
                LocalDate.now().plusMonths(6),
                null, // null supplier
                100
        );

        when(inventoryRepository.findBatchByNumber(batchNumber))
                .thenReturn(Optional.of(batchWithNullSupplier));
        when(inventoryRepository.getPhysicalStoreUsage(batchNumber)).thenReturn(0);
        when(inventoryRepository.getOnlineStoreUsage(batchNumber)).thenReturn(0);
        when(inventoryRepository.batchHasBeenSold(batchNumber)).thenReturn(false);
        // removeBatch returns void, so no need to mock return value

        // When
        CommandResult result = removeBatchCommand.execute();

        // Then
        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("Supplier: Unknown"));
        verify(inventoryRepository).removeBatch(batchNumber);
    }

    @Test
    @DisplayName("Should validate batch removal with edge cases")
    void shouldValidateBatchRemovalWithEdgeCases() {
        // Test case 1: Physical store usage is exactly 0
        when(inventoryRepository.findBatchByNumber(batchNumber))
                .thenReturn(Optional.of(mockBatch));
        when(inventoryRepository.getPhysicalStoreUsage(batchNumber)).thenReturn(0);
        when(inventoryRepository.getOnlineStoreUsage(batchNumber)).thenReturn(1);
        when(inventoryRepository.batchHasBeenSold(batchNumber)).thenReturn(false);

        InventoryException exception1 = assertThrows(InventoryException.class,
                () -> removeBatchCommand.execute());
        assertTrue(exception1.getMessage().contains("online store inventory"));

        // Test case 2: Online store usage is exactly 0
        when(inventoryRepository.getPhysicalStoreUsage(batchNumber)).thenReturn(1);
        when(inventoryRepository.getOnlineStoreUsage(batchNumber)).thenReturn(0);

        InventoryException exception2 = assertThrows(InventoryException.class,
                () -> removeBatchCommand.execute());
        assertTrue(exception2.getMessage().contains("physical store inventory"));

        // Test case 3: Both usages are 0 but batch has been sold
        when(inventoryRepository.getPhysicalStoreUsage(batchNumber)).thenReturn(0);
        when(inventoryRepository.getOnlineStoreUsage(batchNumber)).thenReturn(0);
        when(inventoryRepository.batchHasBeenSold(batchNumber)).thenReturn(true);

        InventoryException exception3 = assertThrows(InventoryException.class,
                () -> removeBatchCommand.execute());
        assertTrue(exception3.getMessage().contains("historical sales transactions"));
    }

    @Test
    @DisplayName("Should handle batch with zero remaining quantity")
    void shouldHandleBatchWithZeroRemainingQuantity() throws InventoryException {
        // Given - Create batch with zero remaining quantity
        MainInventory zeroQuantityBatch = new MainInventory(
                batchNumber,
                new ProductCode("TEST001"),
                100,
                new Money(BigDecimal.valueOf(50.00)),
                LocalDate.now(),
                LocalDate.now().plusMonths(6),
                "Test Supplier",
                0 // zero remaining quantity
        );

        when(inventoryRepository.findBatchByNumber(batchNumber))
                .thenReturn(Optional.of(zeroQuantityBatch));
        when(inventoryRepository.getPhysicalStoreUsage(batchNumber)).thenReturn(0);
        when(inventoryRepository.getOnlineStoreUsage(batchNumber)).thenReturn(0);
        when(inventoryRepository.batchHasBeenSold(batchNumber)).thenReturn(false);
        // removeBatch returns void, so no need to mock return value

        // When
        CommandResult result = removeBatchCommand.execute();

        // Then
        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("Remaining Quantity: 0 units"));
        verify(inventoryRepository).removeBatch(batchNumber);
    }

    @Test
    @DisplayName("Should handle different purchase date formats")
    void shouldHandleDifferentPurchaseDateFormats() throws InventoryException {
        // Given - Create batch with specific purchase date
        LocalDate specificDate = LocalDate.of(2024, 1, 15);
        MainInventory specificDateBatch = new MainInventory(
                batchNumber,
                new ProductCode("TEST001"),
                100,
                new Money(BigDecimal.valueOf(50.00)),
                specificDate,
                specificDate.plusMonths(6),
                "Test Supplier",
                100
        );

        when(inventoryRepository.findBatchByNumber(batchNumber))
                .thenReturn(Optional.of(specificDateBatch));
        when(inventoryRepository.getPhysicalStoreUsage(batchNumber)).thenReturn(0);
        when(inventoryRepository.getOnlineStoreUsage(batchNumber)).thenReturn(0);
        when(inventoryRepository.batchHasBeenSold(batchNumber)).thenReturn(false);
        // removeBatch returns void, so no need to mock return value

        // When
        CommandResult result = removeBatchCommand.execute();

        // Then
        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("Purchase Date: " + specificDate.toString()));
        verify(inventoryRepository).removeBatch(batchNumber);
    }

    @Test
    @DisplayName("Should clear removed batch reference after successful undo")
    void shouldClearRemovedBatchAfterUndo() throws InventoryException {
        // Given
        when(inventoryRepository.findBatchByNumber(batchNumber))
                .thenReturn(Optional.of(mockBatch));
        when(inventoryRepository.getPhysicalStoreUsage(batchNumber)).thenReturn(0);
        when(inventoryRepository.getOnlineStoreUsage(batchNumber)).thenReturn(0);
        when(inventoryRepository.batchHasBeenSold(batchNumber)).thenReturn(false);
        // removeBatch returns void, so no need to mock return value
        when(inventoryRepository.restoreBatch(mockBatch)).thenReturn(mockBatch);

        // Execute and undo
        removeBatchCommand.execute();
        removeBatchCommand.undo();

        // Then - Try to undo again should fail
        CommandResult result = removeBatchCommand.undo();
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Cannot undo: No batch was removed"));
    }

    @Test
    @DisplayName("Should maintain state after failed execution")
    void shouldMaintainStateAfterFailedExecution() {
        // Given - Setup for failure
        when(inventoryRepository.findBatchByNumber(batchNumber))
                .thenReturn(Optional.empty());

        // When & Then - Execute fails
        assertThrows(InventoryException.class, () -> removeBatchCommand.execute());

        // State should not allow undo
        assertFalse(removeBatchCommand.canUndo());

        // Undo should return failure
        CommandResult undoResult = removeBatchCommand.undo();
        assertFalse(undoResult.isSuccess());
    }

    @Test
    @DisplayName("Should handle batch with all validation passing")
    void shouldHandleBatchWithAllValidationPassing() throws InventoryException {
        // Given - All validations pass
        when(inventoryRepository.findBatchByNumber(batchNumber))
                .thenReturn(Optional.of(mockBatch));
        when(inventoryRepository.getPhysicalStoreUsage(batchNumber)).thenReturn(0);
        when(inventoryRepository.getOnlineStoreUsage(batchNumber)).thenReturn(0);
        when(inventoryRepository.batchHasBeenSold(batchNumber)).thenReturn(false);
        // removeBatch returns void, so no need to mock return value

        // When
        CommandResult result = removeBatchCommand.execute();

        // Then
        assertTrue(result.isSuccess());
        verify(inventoryRepository).removeBatch(batchNumber);
    }
}