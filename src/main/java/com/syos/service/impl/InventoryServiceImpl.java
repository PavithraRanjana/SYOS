package com.syos.service.impl;

import com.syos.domain.models.MainInventory;
import com.syos.domain.models.PhysicalStoreInventory;
import com.syos.domain.valueobjects.ProductCode;
import com.syos.exceptions.InsufficientStockException;
import com.syos.repository.interfaces.InventoryRepository;
import com.syos.service.interfaces.InventoryService;

public class InventoryServiceImpl implements InventoryService {
    private final InventoryRepository inventoryRepository;

    public InventoryServiceImpl(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public MainInventory reserveStock(ProductCode productCode, int quantity) {
        return inventoryRepository.findNextAvailableBatch(productCode, quantity)
                .orElseThrow(() -> new InsufficientStockException(
                        "Insufficient stock for product: " + productCode,
                        getTotalAvailableStock(productCode),
                        quantity));
    }

    @Override
    public MainInventory reserveOnlineStock(ProductCode productCode, int quantity) {
        return inventoryRepository.findNextAvailableOnlineBatch(productCode, quantity)
                .orElseThrow(() -> new InsufficientStockException(
                        "Insufficient online stock for product: " + productCode,
                        getTotalOnlineStock(productCode),
                        quantity));
    }

    @Override
    public void reducePhysicalStoreStock(ProductCode productCode, int batchNumber, int quantity) {
        PhysicalStoreInventory storeInventory = inventoryRepository
                .findPhysicalStoreStockByBatch(productCode, batchNumber)
                .orElseThrow(() -> new InsufficientStockException(
                        "No stock found in physical store for batch: " + batchNumber, 0, quantity));

        if (!storeInventory.hasEnoughStock(quantity)) {
            throw new InsufficientStockException(
                    "Insufficient stock in physical store",
                    storeInventory.getQuantityOnShelf(),
                    quantity);
        }

        storeInventory.reduceStock(quantity);
        inventoryRepository.updatePhysicalStoreStock(storeInventory);
    }

    @Override
    public void reduceOnlineStoreStock(ProductCode productCode, int batchNumber, int quantity) {
        // Implementation for online store stock reduction
        // This would be similar to physical store but for online_store_inventory table
        try {
            inventoryRepository.reduceOnlineStock(productCode, batchNumber, quantity);
        } catch (Exception e) {
            throw new InsufficientStockException(
                    "Failed to reduce online stock for batch: " + batchNumber, 0, quantity);
        }
    }

    @Override
    public void reduceMainInventoryStock(int batchNumber, int quantity) {
        // This will be called by database trigger, but we can implement for manual operations
        MainInventory mainInventory = inventoryRepository
                .findMainInventoryBatches(new ProductCode("dummy")) // This needs refactoring
                .stream()
                .filter(batch -> batch.getBatchNumber() == batchNumber)
                .findFirst()
                .orElseThrow(() -> new InsufficientStockException(
                        "Batch not found: " + batchNumber, 0, quantity));

        mainInventory.reduceStock(quantity);
        inventoryRepository.updateMainInventoryStock(mainInventory);
    }

    @Override
    public int getTotalAvailableStock(ProductCode productCode) {
        return inventoryRepository.getTotalPhysicalStock(productCode);
    }

    @Override
    public int getTotalOnlineStock(ProductCode productCode) {
        return inventoryRepository.getTotalOnlineStock(productCode);
    }

    @Override
    public boolean isProductAvailableOnline(ProductCode productCode, int quantity) {
        return getTotalOnlineStock(productCode) >= quantity;
    }

    @Override
    public PhysicalStoreInventory findPhysicalStockByBatch(ProductCode productCode, int batchNumber) {
        return inventoryRepository.findPhysicalStoreStockByBatch(productCode, batchNumber)
                .orElseThrow(() -> new InsufficientStockException(
                        "No physical stock found for batch: " + batchNumber, 0, 0));
    }
}