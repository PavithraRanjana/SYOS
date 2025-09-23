package com.syos.repository.interfaces;

import com.syos.domain.models.PhysicalStoreInventory;
import com.syos.domain.models.MainInventory;
import com.syos.domain.valueobjects.ProductCode;
import com.syos.domain.valueobjects.Money;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Extended InventoryRepository interface with additional methods for inventory manager operations.
 * This extends the existing InventoryRepository with new functionality.
 */
public interface InventoryRepository {

    // ==================== EXISTING METHODS ====================
    List<PhysicalStoreInventory> findPhysicalStoreStock(ProductCode productCode);
    List<MainInventory> findMainInventoryBatches(ProductCode productCode);
    Optional<MainInventory> findNextAvailableBatch(ProductCode productCode, int requiredQuantity);
    Optional<MainInventory> findNextAvailableOnlineBatch(ProductCode productCode, int requiredQuantity);
    Optional<PhysicalStoreInventory> findPhysicalStoreStockByBatch(ProductCode productCode, int batchNumber);
    void updatePhysicalStoreStock(PhysicalStoreInventory inventory);
    void updateMainInventoryStock(MainInventory inventory);
    void reduceOnlineStock(ProductCode productCode, int batchNumber, int quantity);
    int getTotalPhysicalStock(ProductCode productCode);
    int getTotalOnlineStock(ProductCode productCode);

    // ==================== NEW METHODS FOR INVENTORY MANAGER ====================

    /**
     * Adds a new batch to main inventory.
     *
     * @param productCode Product code
     * @param quantityReceived Quantity received
     * @param purchasePrice Purchase price per unit
     * @param purchaseDate Purchase date
     * @param expiryDate Expiry date (can be null)
     * @param supplierName Supplier name
     * @return Created MainInventory batch
     */
    MainInventory addNewBatch(ProductCode productCode,
                              int quantityReceived,
                              Money purchasePrice,
                              LocalDate purchaseDate,
                              LocalDate expiryDate,
                              String supplierName);

    /**
     * Finds a batch by its batch number.
     *
     * @param batchNumber Batch number
     * @return MainInventory batch if found
     */
    Optional<MainInventory> findBatchByNumber(int batchNumber);

    /**
     * Removes a batch from main inventory.
     *
     * @param batchNumber Batch number to remove
     */
    void removeBatch(int batchNumber);

    /**
     * Restores a previously removed batch.
     * Used for undo operations.
     *
     * @param batch Batch to restore
     * @return Restored batch
     */
    MainInventory restoreBatch(MainInventory batch);

    /**
     * Issues stock from main inventory to physical store.
     *
     * @param productCode Product code
     * @param batchNumber Source batch number
     * @param quantity Quantity to issue
     */
    void issueToPhysicalStore(ProductCode productCode, int batchNumber, int quantity);

    /**
     * Issues stock from main inventory to online store.
     *
     * @param productCode Product code
     * @param batchNumber Source batch number
     * @param quantity Quantity to issue
     */
    void issueToOnlineStore(ProductCode productCode, int batchNumber, int quantity);

    /**
     * Returns stock from physical store to main inventory.
     * Used for undo operations.
     *
     * @param productCode Product code
     * @param batchNumber Batch number
     * @param quantity Quantity to return
     */
    void returnFromPhysicalStore(ProductCode productCode, int batchNumber, int quantity);

    /**
     * Returns stock from online store to main inventory.
     * Used for undo operations.
     *
     * @param productCode Product code
     * @param batchNumber Batch number
     * @param quantity Quantity to return
     */
    void returnFromOnlineStore(ProductCode productCode, int batchNumber, int quantity);

    /**
     * Reduces main inventory stock for a specific batch.
     *
     * @param batchNumber Batch number
     * @param quantity Quantity to reduce
     */
    void reduceMainInventoryStock(int batchNumber, int quantity);

    /**
     * Restores main inventory stock for a specific batch.
     * Used for undo operations.
     *
     * @param batchNumber Batch number
     * @param quantity Quantity to restore
     */
    void restoreMainInventoryStock(int batchNumber, int quantity);

    /**
     * Gets total stock in main inventory for a product.
     *
     * @param productCode Product code
     * @return Total quantity in main inventory
     */
    int getTotalMainInventoryStock(ProductCode productCode);

    /**
     * Gets physical store usage for a specific batch.
     *
     * @param batchNumber Batch number
     * @return Quantity currently in physical store from this batch
     */
    int getPhysicalStoreUsage(int batchNumber);

    /**
     * Gets online store usage for a specific batch.
     *
     * @param batchNumber Batch number
     * @return Quantity currently in online store from this batch
     */
    int getOnlineStoreUsage(int batchNumber);

    /**
     * Checks if a batch has been used in any sales.
     *
     * @param batchNumber Batch number
     * @return true if batch has sales history
     */
    boolean batchHasBeenSold(int batchNumber);

    /**
     * Finds batches with low stock (below threshold).
     *
     * @param threshold Minimum quantity threshold
     * @return List of batches below threshold
     */
    List<MainInventory> findLowStockBatches(int threshold);

    /**
     * Finds batches expiring before a specific date.
     *
     * @param beforeDate Date to check before
     * @return List of batches expiring before the date
     */
    List<MainInventory> findBatchesExpiringBefore(LocalDate beforeDate);

    /**
     * Gets all categories.
     *
     * @return List of all categories
     */
    List<CategoryData> findAllCategories();

    /**
     * Gets subcategories for a specific category.
     *
     * @param categoryId Category ID
     * @return List of subcategories
     */
    List<SubcategoryData> findSubcategoriesByCategory(int categoryId);

    /**
     * Gets all brands.
     *
     * @return List of all brands
     */
    List<BrandData> findAllBrands();

    // ==================== DATA TRANSFER OBJECTS ====================

    /**
     * Data transfer object for category information.
     */
    class CategoryData {
        private final int categoryId;
        private final String categoryName;
        private final String categoryCode;

        public CategoryData(int categoryId, String categoryName, String categoryCode) {
            this.categoryId = categoryId;
            this.categoryName = categoryName;
            this.categoryCode = categoryCode;
        }

        public int getCategoryId() { return categoryId; }
        public String getCategoryName() { return categoryName; }
        public String getCategoryCode() { return categoryCode; }
    }

    /**
     * Data transfer object for subcategory information.
     */
    class SubcategoryData {
        private final int subcategoryId;
        private final String subcategoryName;
        private final String subcategoryCode;
        private final int categoryId;

        public SubcategoryData(int subcategoryId, String subcategoryName,
                               String subcategoryCode, int categoryId) {
            this.subcategoryId = subcategoryId;
            this.subcategoryName = subcategoryName;
            this.subcategoryCode = subcategoryCode;
            this.categoryId = categoryId;
        }

        public int getSubcategoryId() { return subcategoryId; }
        public String getSubcategoryName() { return subcategoryName; }
        public String getSubcategoryCode() { return subcategoryCode; }
        public int getCategoryId() { return categoryId; }
    }

    /**
     * Data transfer object for brand information.
     */
    class BrandData {
        private final int brandId;
        private final String brandName;
        private final String brandCode;

        public BrandData(int brandId, String brandName, String brandCode) {
            this.brandId = brandId;
            this.brandName = brandName;
            this.brandCode = brandCode;
        }

        public int getBrandId() { return brandId; }
        public String getBrandName() { return brandName; }
        public String getBrandCode() { return brandCode; }
    }
}