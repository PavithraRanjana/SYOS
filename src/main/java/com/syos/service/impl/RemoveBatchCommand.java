package com.syos.service.impl;

import com.syos.domain.models.MainInventory;
import com.syos.exceptions.InventoryException;
import com.syos.repository.interfaces.InventoryRepository;
import com.syos.service.interfaces.InventoryCommand;

/**
 * Command to remove a batch from main inventory.
 * Implements Command Pattern with undo capability.
 */
public class RemoveBatchCommand implements InventoryCommand {

    private final InventoryRepository inventoryRepository;
    private final int batchNumber;

    private MainInventory removedBatch; // For undo operation

    public RemoveBatchCommand(InventoryRepository inventoryRepository, int batchNumber) {
        this.inventoryRepository = inventoryRepository;
        this.batchNumber = batchNumber;
    }

    @Override
    public CommandResult execute() throws InventoryException {
        try {
            // Get batch details before removal for undo capability
            removedBatch = inventoryRepository.findBatchByNumber(batchNumber)
                    .orElseThrow(() -> new InventoryException("Batch not found: " + batchNumber));

            // Check if batch can be safely removed
            validateBatchRemoval(removedBatch);

            // Remove the batch
            inventoryRepository.removeBatch(batchNumber);

            String message = String.format(
                    "✅ Successfully removed batch #%d\n" +
                            "   Product: %s\n" +
                            "   Remaining Quantity: %d units\n" +
                            "   Supplier: %s\n" +
                            "   Purchase Date: %s",
                    batchNumber,
                    removedBatch.getProductCode().getCode(),
                    removedBatch.getRemainingQuantity(),
                    removedBatch.getSupplierName() != null ? removedBatch.getSupplierName() : "Unknown",
                    removedBatch.getPurchaseDate()
            );

            return CommandResult.success(message, removedBatch);

        } catch (Exception e) {
            throw new InventoryException("Failed to remove batch: " + e.getMessage(), e);
        }
    }

    @Override
    public CommandResult undo() throws InventoryException {
        if (removedBatch == null) {
            return CommandResult.failure("Cannot undo: No batch was removed");
        }

        try {
            // Restore the batch
            MainInventory restoredBatch = inventoryRepository.restoreBatch(removedBatch);

            String message = String.format(
                    "↩️  Successfully undone: Restored batch #%d for product %s",
                    removedBatch.getBatchNumber(),
                    removedBatch.getProductCode().getCode()
            );

            MainInventory batch = removedBatch;
            removedBatch = null; // Clear for future operations

            return CommandResult.success(message, restoredBatch);

        } catch (Exception e) {
            throw new InventoryException("Failed to undo batch removal: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean canUndo() {
        return removedBatch != null;
    }

    @Override
    public String getDescription() {
        return String.format("Remove batch #%d", batchNumber);
    }

    @Override
    public CommandType getCommandType() {
        return CommandType.REMOVE_BATCH;
    }

    private void validateBatchRemoval(MainInventory batch) throws InventoryException {
        // Check if batch is being used in physical store
        int physicalStoreUsage = inventoryRepository.getPhysicalStoreUsage(batch.getBatchNumber());
        if (physicalStoreUsage > 0) {
            throw new InventoryException(
                    String.format("Cannot remove batch #%d: %d units are in physical store inventory",
                            batch.getBatchNumber(), physicalStoreUsage));
        }

        // Check if batch is being used in online store
        int onlineStoreUsage = inventoryRepository.getOnlineStoreUsage(batch.getBatchNumber());
        if (onlineStoreUsage > 0) {
            throw new InventoryException(
                    String.format("Cannot remove batch #%d: %d units are in online store inventory",
                            batch.getBatchNumber(), onlineStoreUsage));
        }

        // Check if batch has been used in any bills (sold items)
        boolean hasBeenSold = inventoryRepository.batchHasBeenSold(batch.getBatchNumber());
        if (hasBeenSold) {
            throw new InventoryException(
                    String.format("Cannot remove batch #%d: This batch has historical sales transactions",
                            batch.getBatchNumber()));
        }
    }
}