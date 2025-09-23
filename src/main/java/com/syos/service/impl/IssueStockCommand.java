package com.syos.service.impl;

import com.syos.domain.models.MainInventory;
import com.syos.domain.valueobjects.ProductCode;
import com.syos.domain.enums.StoreType;
import com.syos.exceptions.InventoryException;
import com.syos.repository.interfaces.InventoryRepository;
import com.syos.service.interfaces.InventoryCommand;

/**
 * Command to issue stock from main inventory to physical or online store.
 * Implements Command Pattern with undo capability.
 */
public class IssueStockCommand implements InventoryCommand {

    private final InventoryRepository inventoryRepository;
    private final ProductCode productCode;
    private final int quantity;
    private final StoreType targetStore;
    private final MainInventory sourceBatch;

    private boolean executed = false;
    private int actualQuantityIssued = 0;

    public IssueStockCommand(InventoryRepository inventoryRepository,
                             ProductCode productCode,
                             int quantity,
                             StoreType targetStore,
                             MainInventory sourceBatch) {
        this.inventoryRepository = inventoryRepository;
        this.productCode = productCode;
        this.quantity = quantity;
        this.targetStore = targetStore;
        this.sourceBatch = sourceBatch;
    }

    @Override
    public CommandResult execute() throws InventoryException {
        try {
            // Validate inputs
            validateIssueRequest();

            // Determine actual quantity to issue (may be less than requested if insufficient stock)
            actualQuantityIssued = Math.min(quantity, sourceBatch.getRemainingQuantity());

            if (actualQuantityIssued == 0) {
                return CommandResult.failure("No stock available in selected batch");
            }

            // Issue stock to target store
            if (targetStore == StoreType.PHYSICAL) {
                inventoryRepository.issueToPhysicalStore(productCode, sourceBatch.getBatchNumber(), actualQuantityIssued);
            } else {
                inventoryRepository.issueToOnlineStore(productCode, sourceBatch.getBatchNumber(), actualQuantityIssued);
            }

            // Reduce main inventory
            inventoryRepository.reduceMainInventoryStock(sourceBatch.getBatchNumber(), actualQuantityIssued);

            executed = true;

            String message = createSuccessMessage();

            return CommandResult.success(message, new IssueResult(productCode, actualQuantityIssued,
                    targetStore, sourceBatch.getBatchNumber()));

        } catch (Exception e) {
            throw new InventoryException("Failed to issue stock: " + e.getMessage(), e);
        }
    }

    @Override
    public CommandResult undo() throws InventoryException {
        if (!executed) {
            return CommandResult.failure("Cannot undo: No stock was issued");
        }

        try {
            // Return stock from target store to main inventory
            if (targetStore == StoreType.PHYSICAL) {
                inventoryRepository.returnFromPhysicalStore(productCode, sourceBatch.getBatchNumber(), actualQuantityIssued);
            } else {
                inventoryRepository.returnFromOnlineStore(productCode, sourceBatch.getBatchNumber(), actualQuantityIssued);
            }

            // Restore main inventory
            inventoryRepository.restoreMainInventoryStock(sourceBatch.getBatchNumber(), actualQuantityIssued);

            String message = String.format(
                    "↩️  Successfully undone stock issue:\n" +
                            "   Returned %d units of %s to main inventory\n" +
                            "   From: %s store\n" +
                            "   Batch: #%d",
                    actualQuantityIssued,
                    productCode.getCode(),
                    targetStore.name().toLowerCase(),
                    sourceBatch.getBatchNumber()
            );

            executed = false;

            return CommandResult.success(message, new IssueResult(productCode, actualQuantityIssued,
                    targetStore, sourceBatch.getBatchNumber()));

        } catch (Exception e) {
            throw new InventoryException("Failed to undo stock issue: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean canUndo() {
        return executed;
    }

    @Override
    public String getDescription() {
        return String.format("Issue %d units of %s to %s store from batch #%d",
                quantity, productCode.getCode(),
                targetStore.name().toLowerCase(),
                sourceBatch.getBatchNumber());
    }

    @Override
    public CommandType getCommandType() {
        return CommandType.ISSUE_STOCK;
    }

    private void validateIssueRequest() throws InventoryException {
        if (quantity <= 0) {
            throw new InventoryException("Issue quantity must be positive");
        }

        if (sourceBatch.getRemainingQuantity() <= 0) {
            throw new InventoryException("Selected batch has no remaining stock");
        }

        if (!sourceBatch.getProductCode().equals(productCode)) {
            throw new InventoryException("Batch product code does not match request");
        }

        // Check if batch is expired
        if (sourceBatch.getExpiryDate() != null &&
                sourceBatch.getExpiryDate().isBefore(java.time.LocalDate.now())) {
            throw new InventoryException("Cannot issue from expired batch (expired: " + sourceBatch.getExpiryDate() + ")");
        }
    }

    private String createSuccessMessage() {
        StringBuilder message = new StringBuilder();
        message.append("✅ Successfully issued stock:\n");
        message.append(String.format("   Product: %s\n", productCode.getCode()));
        message.append(String.format("   Quantity Issued: %d units", actualQuantityIssued));

        if (actualQuantityIssued < quantity) {
            message.append(String.format(" (requested %d)", quantity));
        }

        message.append(String.format("\n   Target: %s store\n", targetStore.name().toLowerCase()));
        message.append(String.format("   From Batch: #%d\n", sourceBatch.getBatchNumber()));
        message.append(String.format("   Batch Remaining: %d units\n",
                sourceBatch.getRemainingQuantity() - actualQuantityIssued));

        // Add expiry warning if applicable
        if (sourceBatch.getExpiryDate() != null) {
            long daysToExpiry = java.time.temporal.ChronoUnit.DAYS.between(
                    java.time.LocalDate.now(), sourceBatch.getExpiryDate());

            if (daysToExpiry <= 30) {
                message.append(String.format("   ⚠️  Batch expires in %d days (%s)\n",
                        daysToExpiry, sourceBatch.getExpiryDate()));
            }
        }

        return message.toString();
    }

    /**
     * Result of stock issue operation.
     */
    public static class IssueResult {
        private final ProductCode productCode;
        private final int quantityIssued;
        private final StoreType targetStore;
        private final int batchNumber;

        public IssueResult(ProductCode productCode, int quantityIssued,
                           StoreType targetStore, int batchNumber) {
            this.productCode = productCode;
            this.quantityIssued = quantityIssued;
            this.targetStore = targetStore;
            this.batchNumber = batchNumber;
        }

        public ProductCode getProductCode() { return productCode; }
        public int getQuantityIssued() { return quantityIssued; }
        public StoreType getTargetStore() { return targetStore; }
        public int getBatchNumber() { return batchNumber; }
    }
}