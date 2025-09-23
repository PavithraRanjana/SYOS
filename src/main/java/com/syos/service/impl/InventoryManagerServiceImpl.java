package com.syos.service.impl;

import com.syos.domain.models.MainInventory;
import com.syos.domain.models.Product;
import com.syos.domain.valueobjects.ProductCode;
import com.syos.domain.valueobjects.Money;
import com.syos.domain.enums.StoreType;
import com.syos.domain.enums.UnitOfMeasure;
import com.syos.exceptions.InventoryException;
import com.syos.repository.interfaces.InventoryRepository;
import com.syos.repository.interfaces.ProductRepository;
import com.syos.service.impl.ProductFactory;
import com.syos.service.impl.AddBatchCommand;
import com.syos.service.impl.RemoveBatchCommand;
import com.syos.service.impl.IssueStockCommand;
import com.syos.service.impl.BatchSelectionContext;
import com.syos.service.impl.FIFOWithExpiryStrategy;
import com.syos.service.interfaces.InventoryCommand;
import com.syos.service.interfaces.InventoryManagerService;
import com.syos.service.interfaces.ProductCodeGenerator;
import java.time.LocalDate;
import java.util.List;

/**
 * Implementation of InventoryManagerService.
 * Orchestrates all inventory manager operations using various design patterns.
 */
public class InventoryManagerServiceImpl implements InventoryManagerService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductFactory productFactory;
    private final ProductCodeGenerator codeGenerator;
    private final BatchSelectionContext batchSelectionContext;

    // Command pattern for undo functionality
    private InventoryCommand lastCommand;

    public InventoryManagerServiceImpl(ProductRepository productRepository,
                                       InventoryRepository inventoryRepository,
                                       ProductCodeGenerator codeGenerator) {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.codeGenerator = codeGenerator;
        this.productFactory = new ProductFactory(codeGenerator, productRepository);

        // Initialize with FIFO+Expiry strategy
        this.batchSelectionContext = new BatchSelectionContext(new FIFOWithExpiryStrategy());
    }

    // ==================== PRODUCT MANAGEMENT ====================

    @Override
    public Product addNewProduct(String productName, int categoryId, int subcategoryId,
                                 int brandId, Money unitPrice, String description,
                                 UnitOfMeasure unitOfMeasure) {

        try {
            // Use Factory Pattern to create product
            Product product = productFactory.createProduct(
                    productName, categoryId, subcategoryId, brandId,
                    unitPrice, description, unitOfMeasure
            );

            // Save to repository
            return productRepository.save(product);

        } catch (Exception e) {
            throw new InventoryException("Failed to add new product: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean productExists(ProductCode productCode) {
        return productRepository.existsById(productCode);
    }

    @Override
    public String previewProductCode(int categoryId, int subcategoryId, int brandId) {
        if (codeGenerator instanceof ProductCodeGeneratorImpl) {
            return ((ProductCodeGeneratorImpl) codeGenerator).previewProductCode(
                    categoryId, subcategoryId, brandId);
        }
        return "Preview not available";
    }

    // ==================== BATCH MANAGEMENT ====================

    @Override
    public InventoryCommand.CommandResult addBatch(ProductCode productCode, int quantityReceived,
                                                   Money purchasePrice, LocalDate purchaseDate,
                                                   LocalDate expiryDate, String supplierName) {

        // Validate product exists
        if (!productExists(productCode)) {
            return InventoryCommand.CommandResult.failure("Product does not exist: " + productCode);
        }

        // Create and execute command
        AddBatchCommand command = new AddBatchCommand(
                inventoryRepository, productCode, quantityReceived,
                purchasePrice, purchaseDate, expiryDate, supplierName
        );

        try {
            InventoryCommand.CommandResult result = command.execute();
            if (result.isSuccess()) {
                lastCommand = command; // Store for undo
            }
            return result;
        } catch (Exception e) {
            return InventoryCommand.CommandResult.failure("Failed to add batch: " + e.getMessage());
        }
    }

    @Override
    public InventoryCommand.CommandResult removeBatch(int batchNumber) {
        // Create and execute command
        RemoveBatchCommand command = new RemoveBatchCommand(inventoryRepository, batchNumber);

        try {
            InventoryCommand.CommandResult result = command.execute();
            if (result.isSuccess()) {
                lastCommand = command; // Store for undo
            }
            return result;
        } catch (Exception e) {
            return InventoryCommand.CommandResult.failure("Failed to remove batch: " + e.getMessage());
        }
    }

    @Override
    public List<MainInventory> getProductBatches(ProductCode productCode) {
        return inventoryRepository.findMainInventoryBatches(productCode);
    }

    // ==================== STOCK ISSUING ====================

    @Override
    public BatchSelectionContext.BatchSelectionResult issueToPhysicalStore(ProductCode productCode, int quantity) {
        return issueStock(productCode, quantity, StoreType.PHYSICAL, true);
    }

    @Override
    public BatchSelectionContext.BatchSelectionResult issueToOnlineStore(ProductCode productCode, int quantity) {
        return issueStock(productCode, quantity, StoreType.ONLINE, true);
    }

    @Override
    public BatchSelectionContext.BatchSelectionResult analyzeBatchSelection(ProductCode productCode, int quantity) {
        // Get available batches
        List<MainInventory> availableBatches = inventoryRepository.findMainInventoryBatches(productCode);

        // Use strategy to select batch (without actually issuing)
        return batchSelectionContext.selectBatch(availableBatches, productCode, quantity);
    }

    private BatchSelectionContext.BatchSelectionResult issueStock(ProductCode productCode,
                                                                  int quantity,
                                                                  StoreType targetStore,
                                                                  boolean executeIssue) {

        // Validate product exists
        if (!productExists(productCode)) {
            return new BatchSelectionContext.BatchSelectionResult(
                    java.util.Optional.empty(),
                    "Product does not exist: " + productCode,
                    "N/A"
            );
        }

        // Get available batches
        List<MainInventory> availableBatches = inventoryRepository.findMainInventoryBatches(productCode);

        if (availableBatches.isEmpty()) {
            return new BatchSelectionContext.BatchSelectionResult(
                    java.util.Optional.empty(),
                    "No batches available for product: " + productCode,
                    batchSelectionContext.getCurrentStrategyName()
            );
        }

        // Use strategy to select batch
        BatchSelectionContext.BatchSelectionResult selectionResult =
                batchSelectionContext.selectBatch(availableBatches, productCode, quantity);

        if (!selectionResult.hasSelection()) {
            return selectionResult;
        }

        if (executeIssue) {
            // Create and execute issue command
            IssueStockCommand command = new IssueStockCommand(
                    inventoryRepository, productCode, quantity, targetStore,
                    selectionResult.getSelectedBatch().get()
            );

            try {
                InventoryCommand.CommandResult commandResult = command.execute();
                if (commandResult.isSuccess()) {
                    lastCommand = command; // Store for undo
                }

                // Enhance the selection result with command execution details
                String enhancedReason = selectionResult.getSelectionReason() +
                        "\n\n📦 Stock Issue Result:\n" + commandResult.getMessage();

                return new BatchSelectionContext.BatchSelectionResult(
                        selectionResult.getSelectedBatch(),
                        enhancedReason,
                        selectionResult.getStrategyUsed()
                );

            } catch (Exception e) {
                return new BatchSelectionContext.BatchSelectionResult(
                        java.util.Optional.empty(),
                        "Failed to issue stock: " + e.getMessage(),
                        selectionResult.getStrategyUsed()
                );
            }
        }

        return selectionResult;
    }

    // ==================== REPORTING ====================

    @Override
    public List<MainInventory> getLowStockReport(int threshold) {
        return inventoryRepository.findLowStockBatches(threshold);
    }

    @Override
    public List<MainInventory> getExpiryReport(int daysAhead) {
        LocalDate checkDate = LocalDate.now().plusDays(daysAhead);
        return inventoryRepository.findBatchesExpiringBefore(checkDate);
    }

    @Override
    public InventoryStatus getInventoryStatus(ProductCode productCode) {
        // Get product info
        Product product = productRepository.findById(productCode)
                .orElseThrow(() -> new InventoryException("Product not found: " + productCode));

        // Get quantities from each location
        int mainTotal = inventoryRepository.getTotalMainInventoryStock(productCode);
        int physicalTotal = inventoryRepository.getTotalPhysicalStock(productCode);
        int onlineTotal = inventoryRepository.getTotalOnlineStock(productCode);

        // Get batch details
        List<MainInventory> batches = inventoryRepository.findMainInventoryBatches(productCode);

        return new InventoryStatus(productCode, product.getProductName(),
                mainTotal, physicalTotal, onlineTotal, batches);
    }

    // ==================== COMMAND MANAGEMENT ====================

    @Override
    public InventoryCommand.CommandResult undoLastCommand() {
        if (lastCommand == null) {
            return InventoryCommand.CommandResult.failure("No command to undo");
        }

        if (!lastCommand.canUndo()) {
            return InventoryCommand.CommandResult.failure("Last command cannot be undone");
        }

        try {
            InventoryCommand.CommandResult result = lastCommand.undo();
            if (result.isSuccess()) {
                lastCommand = null; // Clear after successful undo
            }
            return result;
        } catch (Exception e) {
            return InventoryCommand.CommandResult.failure("Undo failed: " + e.getMessage());
        }
    }

    @Override
    public boolean canUndo() {
        return lastCommand != null && lastCommand.canUndo();
    }

    @Override
    public String getLastCommandDescription() {
        return lastCommand != null ? lastCommand.getDescription() : "No previous command";
    }

    // ==================== MASTER DATA ====================

    @Override
    public List<CategoryInfo> getCategories() {
        return inventoryRepository.findAllCategories().stream()
                .map(cat -> new CategoryInfo(cat.getCategoryId(), cat.getCategoryName(), cat.getCategoryCode()))
                .toList();
    }

    @Override
    public List<SubcategoryInfo> getSubcategories(int categoryId) {
        return inventoryRepository.findSubcategoriesByCategory(categoryId).stream()
                .map(sub -> new SubcategoryInfo(sub.getSubcategoryId(), sub.getSubcategoryName(),
                        sub.getSubcategoryCode(), sub.getCategoryId()))
                .toList();
    }

    @Override
    public List<BrandInfo> getBrands() {
        return inventoryRepository.findAllBrands().stream()
                .map(brand -> new BrandInfo(brand.getBrandId(), brand.getBrandName(), brand.getBrandCode()))
                .toList();
    }
}