package com.syos.service.interfaces;

import com.syos.domain.models.MainInventory;
import com.syos.domain.models.Product;
import com.syos.domain.valueobjects.ProductCode;
import com.syos.domain.valueobjects.Money;
import com.syos.domain.enums.StoreType;
import com.syos.domain.enums.UnitOfMeasure;
import com.syos.service.impl.BatchSelectionContext.BatchSelectionResult;
import com.syos.service.interfaces.InventoryCommand.CommandResult;
import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for inventory manager operations.
 * Provides high-level operations for managing inventory, products, and stock transfers.
 */
public interface InventoryManagerService {

    // ==================== PRODUCT MANAGEMENT ====================

    /**
     * Adds a new product to the system.
     *
     * @param productName Product name
     * @param categoryId Category ID (must exist)
     * @param subcategoryId Subcategory ID (must exist)
     * @param brandId Brand ID (must exist)
     * @param unitPrice Unit price
     * @param description Product description
     * @param unitOfMeasure Unit of measure
     * @return Created product
     */
    Product addNewProduct(String productName,
                          int categoryId,
                          int subcategoryId,
                          int brandId,
                          Money unitPrice,
                          String description,
                          UnitOfMeasure unitOfMeasure);

    /**
     * Checks if a product exists in the system.
     *
     * @param productCode Product code to check
     * @return true if product exists
     */
    boolean productExists(ProductCode productCode);

    /**
     * Previews what the product code will be for given parameters.
     *
     * @param categoryId Category ID
     * @param subcategoryId Subcategory ID
     * @param brandId Brand ID
     * @return Preview of product code
     */
    String previewProductCode(int categoryId, int subcategoryId, int brandId);

    // ==================== BATCH MANAGEMENT ====================

    /**
     * Adds a new batch to main inventory.
     *
     * @param productCode Product code
     * @param quantityReceived Quantity received
     * @param purchasePrice Purchase price per unit
     * @param purchaseDate Date of purchase
     * @param expiryDate Expiry date (can be null)
     * @param supplierName Supplier name
     * @return Command result
     */
    CommandResult addBatch(ProductCode productCode,
                           int quantityReceived,
                           Money purchasePrice,
                           LocalDate purchaseDate,
                           LocalDate expiryDate,
                           String supplierName);

    /**
     * Removes a batch from main inventory.
     *
     * @param batchNumber Batch number to remove
     * @return Command result
     */
    CommandResult removeBatch(int batchNumber);

    /**
     * Gets all batches for a product with detailed information.
     *
     * @param productCode Product code
     * @return List of batches with details
     */
    List<MainInventory> getProductBatches(ProductCode productCode);

    // ==================== STOCK ISSUING ====================

    /**
     * Issues stock from main inventory to physical store.
     *
     * @param productCode Product code
     * @param quantity Quantity to issue
     * @return Batch selection result with details
     */
    BatchSelectionResult issueToPhysicalStore(ProductCode productCode, int quantity);

    /**
     * Issues stock from main inventory to online store.
     *
     * @param productCode Product code
     * @param quantity Quantity to issue
     * @return Batch selection result with details
     */
    BatchSelectionResult issueToOnlineStore(ProductCode productCode, int quantity);

    /**
     * Gets batch selection analysis for issuing stock.
     * Shows which batch would be selected and why, without actually issuing.
     *
     * @param productCode Product code
     * @param quantity Quantity needed
     * @return Batch selection analysis
     */
    BatchSelectionResult analyzeBatchSelection(ProductCode productCode, int quantity);

    // ==================== REPORTING ====================

    /**
     * Gets low stock report for main inventory.
     *
     * @param threshold Minimum quantity threshold
     * @return List of products below threshold
     */
    List<MainInventory> getLowStockReport(int threshold);

    /**
     * Gets expiry report for main inventory.
     *
     * @param daysAhead Days ahead to check for expiry
     * @return List of batches expiring soon
     */
    List<MainInventory> getExpiryReport(int daysAhead);

    /**
     * Gets complete inventory status for a product.
     *
     * @param productCode Product code
     * @return Inventory status across all locations
     */
    InventoryStatus getInventoryStatus(ProductCode productCode);

    // ==================== COMMAND MANAGEMENT ====================

    /**
     * Undoes the last command if possible.
     *
     * @return Command result of undo operation
     */
    CommandResult undoLastCommand();

    /**
     * Checks if undo is available.
     *
     * @return true if last command can be undone
     */
    boolean canUndo();

    /**
     * Gets description of the last command.
     *
     * @return Description of last command
     */
    String getLastCommandDescription();

    // ==================== MASTER DATA ====================

    /**
     * Gets all available categories.
     *
     * @return List of categories with IDs and names
     */
    List<CategoryInfo> getCategories();

    /**
     * Gets subcategories for a category.
     *
     * @param categoryId Category ID
     * @return List of subcategories
     */
    List<SubcategoryInfo> getSubcategories(int categoryId);

    /**
     * Gets all available brands.
     *
     * @return List of brands with IDs and names
     */
    List<BrandInfo> getBrands();

    // ==================== HELPER CLASSES ====================

    /**
     * Complete inventory status for a product.
     */
    class InventoryStatus {
        private final ProductCode productCode;
        private final String productName;
        private final int mainInventoryTotal;
        private final int physicalStoreTotal;
        private final int onlineStoreTotal;
        private final List<MainInventory> batches;

        public InventoryStatus(ProductCode productCode, String productName,
                               int mainInventoryTotal, int physicalStoreTotal,
                               int onlineStoreTotal, List<MainInventory> batches) {
            this.productCode = productCode;
            this.productName = productName;
            this.mainInventoryTotal = mainInventoryTotal;
            this.physicalStoreTotal = physicalStoreTotal;
            this.onlineStoreTotal = onlineStoreTotal;
            this.batches = batches;
        }

        public ProductCode getProductCode() { return productCode; }
        public String getProductName() { return productName; }
        public int getMainInventoryTotal() { return mainInventoryTotal; }
        public int getPhysicalStoreTotal() { return physicalStoreTotal; }
        public int getOnlineStoreTotal() { return onlineStoreTotal; }
        public List<MainInventory> getBatches() { return batches; }

        public int getTotalStock() {
            return mainInventoryTotal + physicalStoreTotal + onlineStoreTotal;
        }
    }

    /**
     * Category information.
     */
    class CategoryInfo {
        private final int categoryId;
        private final String categoryName;
        private final String categoryCode;

        public CategoryInfo(int categoryId, String categoryName, String categoryCode) {
            this.categoryId = categoryId;
            this.categoryName = categoryName;
            this.categoryCode = categoryCode;
        }

        public int getCategoryId() { return categoryId; }
        public String getCategoryName() { return categoryName; }
        public String getCategoryCode() { return categoryCode; }

        @Override
        public String toString() {
            return String.format("%d. %s (%s)", categoryId, categoryName, categoryCode);
        }
    }

    /**
     * Subcategory information.
     */
    class SubcategoryInfo {
        private final int subcategoryId;
        private final String subcategoryName;
        private final String subcategoryCode;
        private final int categoryId;

        public SubcategoryInfo(int subcategoryId, String subcategoryName,
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

        @Override
        public String toString() {
            return String.format("%d. %s (%s)", subcategoryId, subcategoryName, subcategoryCode);
        }
    }

    /**
     * Brand information.
     */
    class BrandInfo {
        private final int brandId;
        private final String brandName;
        private final String brandCode;

        public BrandInfo(int brandId, String brandName, String brandCode) {
            this.brandId = brandId;
            this.brandName = brandName;
            this.brandCode = brandCode;
        }

        public int getBrandId() { return brandId; }
        public String getBrandName() { return brandName; }
        public String getBrandCode() { return brandCode; }

        @Override
        public String toString() {
            return String.format("%d. %s (%s)", brandId, brandName, brandCode);
        }
    }
}