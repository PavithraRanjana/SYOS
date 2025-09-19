package com.syos.service.interfaces;

import com.syos.domain.models.MainInventory;
import com.syos.domain.models.PhysicalStoreInventory;
import com.syos.domain.valueobjects.ProductCode;

public interface InventoryService {
    MainInventory reserveStock(ProductCode productCode, int quantity);
    MainInventory reserveOnlineStock(ProductCode productCode, int quantity);
    void reducePhysicalStoreStock(ProductCode productCode, int batchNumber, int quantity);
    void reduceOnlineStoreStock(ProductCode productCode, int batchNumber, int quantity);
    void reduceMainInventoryStock(int batchNumber, int quantity);
    int getTotalAvailableStock(ProductCode productCode);
    int getTotalOnlineStock(ProductCode productCode);
    boolean isProductAvailableOnline(ProductCode productCode, int quantity);
    PhysicalStoreInventory findPhysicalStockByBatch(ProductCode productCode, int batchNumber);
}