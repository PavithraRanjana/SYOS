package com.syos.service.impl;

import com.syos.domain.models.MainInventory;
import com.syos.domain.valueobjects.ProductCode;
import com.syos.domain.valueobjects.Money;
import com.syos.exceptions.InventoryException;
import com.syos.repository.interfaces.InventoryRepository;
import com.syos.service.interfaces.InventoryCommand;
import java.time.LocalDate;

/**
 * Command to add a new batch to main inventory.
 * Implements Command Pattern for inventory operations.
 */
public class AddBatchCommand implements InventoryCommand {

    private final InventoryRepository inventoryRepository;
    private final ProductCode productCode;
    private final int quantityReceived;
    private final Money purchasePrice;
    private final LocalDate purchaseDate;
    private final LocalDate expiryDate;
    private final String supplierName;

    private MainInventory createdBatch; // For undo operation

    public AddBatchCommand(InventoryRepository inventoryRepository,
                           ProductCode productCode,
                           int quantityReceived,
                           Money purchasePrice,
                           LocalDate purchaseDate,
                           LocalDate expiryDate,
                           String supplierName) {
        this.inventoryRepository = inventoryRepository;
        this.productCode = productCode;
        this.quantityReceived = quantityReceived;
        this.purchasePrice = purchasePrice;
        this.purchaseDate = purchaseDate;
        this.expiryDate = expiryDate;
        this.supplierName = supplierName;
    }

    @Override
    public CommandResult execute() throws InventoryException {
        try {
            // Validate inputs
            validateInputs();

            // Create and save the batch
            createdBatch = inventoryRepository.addNewBatch(
                    productCode, quantityReceived, purchasePrice,
                    purchaseDate, expiryDate, supplierName
            );

            String message = String.format(
                    "✅ Successfully added batch #%d for product %s\n" +
                            "   Quantity: %d units\n" +
                            "   Purchase Price: %s per unit\n" +
                            "   Purchase Date: %s\n" +
                            "   Expiry Date: %s\n" +
                            "   Supplier: %s",
                    createdBatch.getBatchNumber(),
                    productCode.getCode(),
                    quantityReceived,
                    purchasePrice,
                    purchaseDate,
                    expiryDate != null ? expiryDate : "No expiry",
                    supplierName != null ? supplierName : "Unknown"
            );

            return CommandResult.success(message, createdBatch);

        } catch (Exception e) {
            throw new InventoryException("Failed to add batch: " + e.getMessage(), e);
        }
    }

    @Override
    public CommandResult undo() throws InventoryException {
        if (createdBatch == null) {
            return CommandResult.failure("Cannot undo: No batch was created");
        }

        try {
            // Remove the batch that was created
            inventoryRepository.removeBatch(createdBatch.getBatchNumber());

            String message = String.format(
                    "↩️  Successfully undone: Removed batch #%d for product %s",
                    createdBatch.getBatchNumber(),
                    productCode.getCode()
            );

            MainInventory removedBatch = createdBatch;
            createdBatch = null; // Clear for future operations

            return CommandResult.success(message, removedBatch);

        } catch (Exception e) {
            throw new InventoryException("Failed to undo batch addition: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean canUndo() {
        return createdBatch != null;
    }

    @Override
    public String getDescription() {
        return String.format("Add batch for product %s: %d units from %s",
                productCode.getCode(), quantityReceived,
                supplierName != null ? supplierName : "Unknown Supplier");
    }

    @Override
    public CommandType getCommandType() {
        return CommandType.ADD_BATCH;
    }

    private void validateInputs() throws InventoryException {
        if (quantityReceived <= 0) {
            throw new InventoryException("Quantity received must be positive");
        }

        if (purchasePrice.getAmount().doubleValue() <= 0) {
            throw new InventoryException("Purchase price must be positive");
        }

        if (purchaseDate == null) {
            throw new InventoryException("Purchase date is required");
        }

        if (purchaseDate.isAfter(LocalDate.now())) {
            throw new InventoryException("Purchase date cannot be in the future");
        }

        if (expiryDate != null && expiryDate.isBefore(purchaseDate)) {
            throw new InventoryException("Expiry date cannot be before purchase date");
        }
    }
}