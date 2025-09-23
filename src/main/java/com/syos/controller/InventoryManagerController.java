package com.syos.controller;

import com.syos.domain.models.MainInventory;
import com.syos.domain.models.Product;
import com.syos.domain.valueobjects.ProductCode;
import com.syos.domain.valueobjects.Money;
import com.syos.domain.enums.UnitOfMeasure;
import com.syos.service.interfaces.InventoryManagerService;
import com.syos.service.interfaces.InventoryCommand.CommandResult;
import com.syos.service.impl.BatchSelectionContext.BatchSelectionResult;
import com.syos.ui.interfaces.UserInterface;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Controller for Inventory Manager operations.
 * Provides CLI interface for all inventory management functions.
 */
public class InventoryManagerController {

    private final InventoryManagerService inventoryManagerService;
    private final UserInterface ui;

    public InventoryManagerController(InventoryManagerService inventoryManagerService,
                                      UserInterface ui) {
        this.inventoryManagerService = inventoryManagerService;
        this.ui = ui;
    }

    public void startInventoryManagerMode() {
        ui.clearScreen();
        displayWelcome();

        boolean running = true;
        while (running) {
            try {
                displayMainMenu();
                String choice = ui.getUserInput();

                switch (choice) {
                    case "1" -> addNewProduct();
                    case "2" -> addNewBatch();
                    case "3" -> removeBatch();
                    case "4" -> issueStockToPhysicalStore();
                    case "5" -> issueStockToOnlineStore();
                    case "6" -> viewBatchSelection();
                    case "7" -> viewInventoryReports();
                    case "8" -> viewProductStatus();
                    case "9" -> undoLastOperation();
                    case "10" -> {
                        ui.displaySuccess("Returning to main menu...");
                        running = false;
                    }
                    default -> ui.displayError("Invalid option. Please try again.");
                }
            } catch (Exception e) {
                ui.displayError("Unexpected error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void displayWelcome() {
        System.out.println("================================================");
        System.out.println("         SYOS - Inventory Manager");
        System.out.println("================================================");
        System.out.println("Manage products, batches, and stock transfers");
        System.out.println("================================================");
    }

    private void displayMainMenu() {
        ui.clearScreen();
        System.out.println("================================================");
        System.out.println("         SYOS - Inventory Manager");
        System.out.println("================================================");
        System.out.println();
        System.out.println("=== INVENTORY MANAGEMENT MENU ===");
        System.out.println("1. 📦 Add New Product");
        System.out.println("2. 📥 Add New Batch to Main Inventory");
        System.out.println("3. 🗑️  Remove Batch from Main Inventory");
        System.out.println("4. 🏪 Issue Stock to Physical Store");
        System.out.println("5. 🌐 Issue Stock to Online Store");
        System.out.println("6. 🔍 View Batch Selection Analysis");
        System.out.println("7. 📊 View Inventory Reports");
        System.out.println("8. 📋 View Product Inventory Status");
        System.out.println("9. ↩️  Undo Last Operation");
        System.out.println("10. ⬅️ Back to Main Menu");
        System.out.println();
        System.out.print("Select option: ");
    }

    private void addNewProduct() {
        ui.clearScreen();
        System.out.println("=== ADD NEW PRODUCT ===");

        try {
            // Display available categories
            displayCategories();
            int categoryId = getIntegerInput("Enter Category ID: ");

            // Display subcategories for selected category
            displaySubcategories(categoryId);
            int subcategoryId = getIntegerInput("Enter Subcategory ID: ");

            // Display brands
            displayBrands();
            int brandId = getIntegerInput("Enter Brand ID: ");

            // Preview product code
            String preview = inventoryManagerService.previewProductCode(categoryId, subcategoryId, brandId);
            System.out.println("📋 Product code will be: " + preview);
            System.out.println();

            // Get product details
            String productName = ui.getUserInput("Enter product name: ");
            if (productName.trim().isEmpty()) {
                ui.displayError("Product name cannot be empty");
                return;
            }

            BigDecimal priceAmount = getBigDecimalInput("Enter unit price (LKR): ");
            Money unitPrice = new Money(priceAmount);

            String description = ui.getUserInput("Enter description (optional): ");

            // Get unit of measure
            UnitOfMeasure unitOfMeasure = selectUnitOfMeasure();

            // Confirm details
            System.out.println("\n=== PRODUCT DETAILS CONFIRMATION ===");
            System.out.println("Product Code: " + preview);
            System.out.println("Product Name: " + productName);
            System.out.println("Unit Price: " + unitPrice);
            System.out.println("Description: " + (description.isEmpty() ? "No description" : description));
            System.out.println("Unit of Measure: " + unitOfMeasure.getValue());

            if (!ui.confirmAction("Create this product?")) {
                ui.displaySuccess("Product creation cancelled.");
                return;
            }

            // Create product
            Product product = inventoryManagerService.addNewProduct(
                    productName, categoryId, subcategoryId, brandId,
                    unitPrice, description.isEmpty() ? null : description, unitOfMeasure
            );

            ui.displaySuccess("✅ Product created successfully!");
            System.out.println("Product Code: " + product.getProductCode());
            System.out.println("Product Name: " + product.getProductName());

        } catch (Exception e) {
            ui.displayError("Failed to add product: " + e.getMessage());
        }

        ui.waitForEnter();
    }

    private void addNewBatch() {
        ui.clearScreen();
        System.out.println("=== ADD NEW BATCH ===");

        try {
            // Get product code
            ProductCode productCode = getProductCodeInput();

            // Validate product exists
            if (!inventoryManagerService.productExists(productCode)) {
                ui.displayError("Product does not exist: " + productCode);
                ui.displaySuccess("💡 Use option 1 to add new products first.");
                return;
            }

            // Get batch details
            int quantity = getIntegerInput("Enter quantity received: ");
            if (quantity <= 0) {
                ui.displayError("Quantity must be positive");
                return;
            }

            BigDecimal purchasePriceAmount = getBigDecimalInput("Enter purchase price per unit (LKR): ");
            Money purchasePrice = new Money(purchasePriceAmount);

            LocalDate purchaseDate = getDateInput("Enter purchase date (YYYY-MM-DD): ");

            System.out.print("Enter expiry date (YYYY-MM-DD, or press Enter if no expiry): ");
            String expiryInput = ui.getUserInput();
            LocalDate expiryDate = null;
            if (!expiryInput.trim().isEmpty()) {
                try {
                    expiryDate = LocalDate.parse(expiryInput);
                } catch (DateTimeParseException e) {
                    ui.displayError("Invalid expiry date format");
                    return;
                }
            }

            String supplier = ui.getUserInput("Enter supplier name (optional): ");

            // Display batch summary
            System.out.println("\n=== BATCH SUMMARY ===");
            System.out.println("Product: " + productCode);
            System.out.println("Quantity: " + quantity + " units");
            System.out.println("Purchase Price: " + purchasePrice + " per unit");
            System.out.println("Total Cost: " + purchasePrice.multiply(quantity));
            System.out.println("Purchase Date: " + purchaseDate);
            System.out.println("Expiry Date: " + (expiryDate != null ? expiryDate : "No expiry"));
            System.out.println("Supplier: " + (supplier.isEmpty() ? "Unknown" : supplier));

            if (!ui.confirmAction("Add this batch?")) {
                ui.displaySuccess("Batch creation cancelled.");
                return;
            }

            // Add batch
            CommandResult result = inventoryManagerService.addBatch(
                    productCode, quantity, purchasePrice, purchaseDate, expiryDate,
                    supplier.isEmpty() ? null : supplier
            );

            if (result.isSuccess()) {
                ui.displaySuccess(result.getMessage());
            } else {
                ui.displayError(result.getMessage());
            }

        } catch (Exception e) {
            ui.displayError("Failed to add batch: " + e.getMessage());
        }

        ui.waitForEnter();
    }

    private void removeBatch() {
        ui.clearScreen();
        System.out.println("=== REMOVE BATCH ===");

        try {
            int batchNumber = getIntegerInput("Enter batch number to remove: ");

            // Show batch details first
            System.out.println("🔍 Looking up batch details...");

            if (!ui.confirmAction("⚠️ Are you sure you want to remove batch #" + batchNumber + "?")) {
                ui.displaySuccess("Batch removal cancelled.");
                return;
            }

            CommandResult result = inventoryManagerService.removeBatch(batchNumber);

            if (result.isSuccess()) {
                ui.displaySuccess(result.getMessage());
            } else {
                ui.displayError(result.getMessage());
            }

        } catch (Exception e) {
            ui.displayError("Failed to remove batch: " + e.getMessage());
        }

        ui.waitForEnter();
    }

    private void issueStockToPhysicalStore() {
        issueStock("PHYSICAL STORE");
    }

    private void issueStockToOnlineStore() {
        issueStock("ONLINE STORE");
    }

    private void issueStock(String storeTypeName) {
        ui.clearScreen();
        System.out.println("=== ISSUE STOCK TO " + storeTypeName + " ===");

        try {
            ProductCode productCode = getProductCodeInput();

            if (!inventoryManagerService.productExists(productCode)) {
                ui.displayError("Product does not exist: " + productCode);
                return;
            }

            // Show batch analysis first
            int quantity = getIntegerInput("Enter quantity to issue: ");

            System.out.println("🔍 Analyzing batch selection...");
            BatchSelectionResult analysis = inventoryManagerService.analyzeBatchSelection(productCode, quantity);

            System.out.println("\n" + "=".repeat(60));
            System.out.println("BATCH SELECTION ANALYSIS");
            System.out.println("=".repeat(60));
            System.out.println("Strategy: " + analysis.getStrategyUsed());
            System.out.println();
            System.out.println(analysis.getSelectionReason());
            System.out.println("=".repeat(60));

            if (!analysis.hasSelection()) {
                ui.displayError("Cannot issue stock - no suitable batch available");
                return;
            }

            if (!ui.confirmAction("Proceed with stock issue?")) {
                ui.displaySuccess("Stock issue cancelled.");
                return;
            }

            // Issue the stock
            BatchSelectionResult result;
            if (storeTypeName.equals("PHYSICAL STORE")) {
                result = inventoryManagerService.issueToPhysicalStore(productCode, quantity);
            } else {
                result = inventoryManagerService.issueToOnlineStore(productCode, quantity);
            }

            System.out.println("\n" + "=".repeat(60));
            System.out.println("STOCK ISSUE RESULT");
            System.out.println("=".repeat(60));
            System.out.println(result.getSelectionReason());
            System.out.println("=".repeat(60));

        } catch (Exception e) {
            ui.displayError("Failed to issue stock: " + e.getMessage());
        }

        ui.waitForEnter();
    }

    private void viewBatchSelection() {
        ui.clearScreen();
        System.out.println("=== BATCH SELECTION ANALYSIS ===");

        try {
            ProductCode productCode = getProductCodeInput();

            if (!inventoryManagerService.productExists(productCode)) {
                ui.displayError("Product does not exist: " + productCode);
                return;
            }

            int quantity = getIntegerInput("Enter quantity for analysis: ");

            BatchSelectionResult result = inventoryManagerService.analyzeBatchSelection(productCode, quantity);

            System.out.println("\n" + "=".repeat(60));
            System.out.println("BATCH SELECTION ANALYSIS");
            System.out.println("=".repeat(60));
            System.out.println("Product: " + productCode);
            System.out.println("Requested Quantity: " + quantity);
            System.out.println("Strategy: " + result.getStrategyUsed());
            System.out.println();
            System.out.println(result.getSelectionReason());
            System.out.println("=".repeat(60));

        } catch (Exception e) {
            ui.displayError("Failed to analyze batch selection: " + e.getMessage());
        }

        ui.waitForEnter();
    }

    private void viewInventoryReports() {
        ui.clearScreen();
        System.out.println("=== INVENTORY REPORTS ===");
        System.out.println("1. 📉 Low Stock Report");
        System.out.println("2. ⏰ Expiry Report");
        System.out.println("3. ⬅️ Back");

        String choice = ui.getUserInput("Select report: ");

        switch (choice) {
            case "1" -> showLowStockReport();
            case "2" -> showExpiryReport();
            case "3" -> { return; }
            default -> ui.displayError("Invalid option");
        }

        ui.waitForEnter();
    }

    private void showLowStockReport() {
        try {
            int threshold = getIntegerInput("Enter minimum stock threshold (default 50): ");
            if (threshold <= 0) threshold = 50;

            List<MainInventory> lowStockBatches = inventoryManagerService.getLowStockReport(threshold);

            System.out.println("\n=== LOW STOCK REPORT ===");
            System.out.println("Threshold: " + threshold + " units");
            System.out.println();

            if (lowStockBatches.isEmpty()) {
                System.out.println("✅ No low stock items found!");
            } else {
                System.out.printf("%-15s %-10s %-15s %-12s %s\n",
                        "Product", "Batch", "Remaining", "Expiry", "Supplier");
                System.out.println("-".repeat(70));

                for (MainInventory batch : lowStockBatches) {
                    System.out.printf("%-15s %-10d %-15d %-12s %s\n",
                            batch.getProductCode().getCode(),
                            batch.getBatchNumber(),
                            batch.getRemainingQuantity(),
                            batch.getExpiryDate() != null ? batch.getExpiryDate().toString() : "No expiry",
                            batch.getSupplierName() != null ? batch.getSupplierName() : "Unknown");
                }
            }
        } catch (Exception e) {
            ui.displayError("Failed to generate low stock report: " + e.getMessage());
        }
    }

    private void showExpiryReport() {
        try {
            int daysAhead = getIntegerInput("Enter days ahead to check (default 30): ");
            if (daysAhead <= 0) daysAhead = 30;

            List<MainInventory> expiringBatches = inventoryManagerService.getExpiryReport(daysAhead);

            System.out.println("\n=== EXPIRY REPORT ===");
            System.out.println("Checking expiry within " + daysAhead + " days");
            System.out.println();

            if (expiringBatches.isEmpty()) {
                System.out.println("✅ No items expiring soon!");
            } else {
                System.out.printf("%-15s %-10s %-12s %-15s %s\n",
                        "Product", "Batch", "Expiry", "Remaining", "Supplier");
                System.out.println("-".repeat(70));

                for (MainInventory batch : expiringBatches) {
                    long daysToExpiry = java.time.temporal.ChronoUnit.DAYS.between(
                            LocalDate.now(), batch.getExpiryDate());
                    String urgency = daysToExpiry <= 7 ? " ⚠️ URGENT" : "";

                    System.out.printf("%-15s %-10d %-12s %-15d %s%s\n",
                            batch.getProductCode().getCode(),
                            batch.getBatchNumber(),
                            batch.getExpiryDate().toString(),
                            batch.getRemainingQuantity(),
                            batch.getSupplierName() != null ? batch.getSupplierName() : "Unknown",
                            urgency);
                }
            }
        } catch (Exception e) {
            ui.displayError("Failed to generate expiry report: " + e.getMessage());
        }
    }

    private void viewProductStatus() {
        ui.clearScreen();
        System.out.println("=== PRODUCT INVENTORY STATUS ===");

        try {
            ProductCode productCode = getProductCodeInput();

            if (!inventoryManagerService.productExists(productCode)) {
                ui.displayError("Product does not exist: " + productCode);
                return;
            }

            InventoryManagerService.InventoryStatus status = inventoryManagerService.getInventoryStatus(productCode);

            System.out.println("\n" + "=".repeat(60));
            System.out.println("INVENTORY STATUS: " + status.getProductName());
            System.out.println("Product Code: " + status.getProductCode());
            System.out.println("=".repeat(60));
            System.out.println();

            // Summary
            System.out.println("📊 STOCK SUMMARY:");
            System.out.println("   Main Inventory: " + status.getMainInventoryTotal() + " units");
            System.out.println("   Physical Store: " + status.getPhysicalStoreTotal() + " units");
            System.out.println("   Online Store: " + status.getOnlineStoreTotal() + " units");
            System.out.println("   TOTAL: " + status.getTotalStock() + " units");
            System.out.println();

            // Batch details
            if (status.getBatches().isEmpty()) {
                System.out.println("❌ No batches found in main inventory");
            } else {
                System.out.println("📦 BATCH DETAILS:");
                System.out.printf("%-8s %-12s %-12s %-12s %-15s %s\n",
                        "Batch", "Remaining", "Purchase", "Expiry", "Supplier", "Status");
                System.out.println("-".repeat(80));

                for (MainInventory batch : status.getBatches()) {
                    String statusText = "";
                    if (batch.getExpiryDate() != null) {
                        long daysToExpiry = java.time.temporal.ChronoUnit.DAYS.between(
                                LocalDate.now(), batch.getExpiryDate());
                        if (daysToExpiry < 0) {
                            statusText = "EXPIRED";
                        } else if (daysToExpiry <= 7) {
                            statusText = "URGENT";
                        } else if (daysToExpiry <= 30) {
                            statusText = "SOON";
                        } else {
                            statusText = "OK";
                        }
                    } else {
                        statusText = "NO EXPIRY";
                    }

                    System.out.printf("%-8d %-12d %-12s %-12s %-15s %s\n",
                            batch.getBatchNumber(),
                            batch.getRemainingQuantity(),
                            batch.getPurchaseDate(),
                            batch.getExpiryDate() != null ? batch.getExpiryDate().toString() : "None",
                            batch.getSupplierName() != null ? batch.getSupplierName() : "Unknown",
                            statusText);
                }
            }

            System.out.println("=".repeat(60));

        } catch (Exception e) {
            ui.displayError("Failed to get product status: " + e.getMessage());
        }

        ui.waitForEnter();
    }

    private void undoLastOperation() {
        ui.clearScreen();
        System.out.println("=== UNDO LAST OPERATION ===");

        if (!inventoryManagerService.canUndo()) {
            ui.displayError("No operation to undo");
            ui.waitForEnter();
            return;
        }

        String lastCommand = inventoryManagerService.getLastCommandDescription();
        System.out.println("Last operation: " + lastCommand);
        System.out.println();

        if (!ui.confirmAction("⚠️ Undo this operation?")) {
            ui.displaySuccess("Undo cancelled.");
            ui.waitForEnter();
            return;
        }

        try {
            CommandResult result = inventoryManagerService.undoLastCommand();

            if (result.isSuccess()) {
                ui.displaySuccess(result.getMessage());
            } else {
                ui.displayError(result.getMessage());
            }

        } catch (Exception e) {
            ui.displayError("Failed to undo operation: " + e.getMessage());
        }

        ui.waitForEnter();
    }

    // ==================== HELPER METHODS ====================

    private void displayCategories() {
        System.out.println("Available Categories:");
        List<InventoryManagerService.CategoryInfo> categories = inventoryManagerService.getCategories();
        for (InventoryManagerService.CategoryInfo category : categories) {
            System.out.println("  " + category);
        }
        System.out.println();
    }

    private void displaySubcategories(int categoryId) {
        System.out.println("Available Subcategories:");
        List<InventoryManagerService.SubcategoryInfo> subcategories =
                inventoryManagerService.getSubcategories(categoryId);
        if (subcategories.isEmpty()) {
            System.out.println("  No subcategories found for category " + categoryId);
        } else {
            for (InventoryManagerService.SubcategoryInfo subcategory : subcategories) {
                System.out.println("  " + subcategory);
            }
        }
        System.out.println();
    }

    private void displayBrands() {
        System.out.println("Available Brands:");
        List<InventoryManagerService.BrandInfo> brands = inventoryManagerService.getBrands();
        for (InventoryManagerService.BrandInfo brand : brands) {
            System.out.println("  " + brand);
        }
        System.out.println();
    }

    private UnitOfMeasure selectUnitOfMeasure() {
        System.out.println("Select Unit of Measure:");
        UnitOfMeasure[] units = UnitOfMeasure.values();
        for (int i = 0; i < units.length; i++) {
            System.out.printf("%d. %s\n", i + 1, units[i].getValue());
        }

        while (true) {
            try {
                int choice = getIntegerInput("Enter choice (1-" + units.length + "): ");
                if (choice >= 1 && choice <= units.length) {
                    return units[choice - 1];
                }
                ui.displayError("Invalid choice. Please try again.");
            } catch (Exception e) {
                ui.displayError("Invalid input. Please enter a number.");
            }
        }
    }

    private ProductCode getProductCodeInput() {
        while (true) {
            try {
                String input = ui.getUserInput("Enter product code: ");
                return new ProductCode(input);
            } catch (IllegalArgumentException e) {
                ui.displayError("Invalid product code: " + e.getMessage());
            }
        }
    }

    private int getIntegerInput(String prompt) {
        while (true) {
            try {
                String input = ui.getUserInput(prompt);
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                ui.displayError("Invalid number. Please try again.");
            }
        }
    }

    private BigDecimal getBigDecimalInput(String prompt) {
        while (true) {
            try {
                String input = ui.getUserInput(prompt);
                BigDecimal value = new BigDecimal(input);
                if (value.compareTo(BigDecimal.ZERO) <= 0) {
                    ui.displayError("Amount must be positive");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                ui.displayError("Invalid amount. Please enter a valid number.");
            }
        }
    }

    private LocalDate getDateInput(String prompt) {
        while (true) {
            try {
                String input = ui.getUserInput(prompt);
                LocalDate date = LocalDate.parse(input);
                if (date.isAfter(LocalDate.now())) {
                    ui.displayError("Date cannot be in the future");
                    continue;
                }
                return date;
            } catch (DateTimeParseException e) {
                ui.displayError("Invalid date format. Please use YYYY-MM-DD format.");
            }
        }
    }
}