package com.syos.repository.interfaces;

import com.syos.domain.models.PhysicalStoreInventory;
import com.syos.domain.models.MainInventory;
import com.syos.domain.valueobjects.ProductCode;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository {
    List<PhysicalStoreInventory> findPhysicalStoreStock(ProductCode productCode);
    List<MainInventory> findMainInventoryBatches(ProductCode productCode);
    Optional<MainInventory> findNextAvailableBatch(ProductCode productCode, int requiredQuantity);
    Optional<PhysicalStoreInventory> findPhysicalStoreStockByBatch(ProductCode productCode, int batchNumber);
    void updatePhysicalStoreStock(PhysicalStoreInventory inventory);
    void updateMainInventoryStock(MainInventory inventory);
    int getTotalPhysicalStock(ProductCode productCode);
}
