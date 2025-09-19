package com.syos.controller;

import com.syos.domain.models.Bill;
import com.syos.domain.models.BillItem;
import com.syos.domain.models.Product;
import com.syos.domain.models.Customer;
import com.syos.domain.valueobjects.ProductCode;
import com.syos.domain.valueobjects.Money;
import com.syos.domain.enums.StoreType;
import com.syos.exceptions.ProductNotFoundException;
import com.syos.exceptions.InsufficientStockException;
import com.syos.exceptions.BillingException;
import com.syos.service.interfaces.BillingService;
import com.syos.service.interfaces.ProductService;
import com.syos.service.interfaces.OnlineStoreService;
import com.syos.service.interfaces.CustomerService;
import com.syos.ui.interfaces.UserInterface;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class OnlineCustomerController {
    private final BillingService billingService;
    private final ProductService productService;
    private final OnlineStoreService onlineStoreService;
    private final CustomerService customerService;
    private final UserInterface ui;

    private Customer currentCustomer;

    public OnlineCustomerController(BillingService billingService,
                                    ProductService productService,
                                    OnlineStoreService onlineStoreService,
                                    CustomerService customerService,
                                    UserInterface ui) {
        this.billingService = billingService;
        this.productService = productService;
        this.onlineStoreService = onlineStoreService;
        this.customerService = customerService;
        this.ui = ui;
    }

    public void start() {
        displayWelcome();

        // Customer authentication/registration
        if (!authenticateCustomer()) {
            return;
        }

        boolean shopping = true;
        while (shopping) {
            try {
                displayOnlineMenu();
                String choice = ui.getUserInput();

                switch (choice) {
                    case "1" -> browseProductsByCategory();
                    case "2" -> searchProducts();
                    case "3" -> startShopping();
                    case "4" -> viewMyOrders();
                    case "5" -> {
                        ui.displaySuccess("Thank you for shopping with SYOS!");
                        shopping = false;
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
        ui.clearScreen();
        System.out.println("================================================");
        System.out.println("           SYOS Online Store");
        System.out.println("================================================");
        System.out.println("Welcome to SYOS Online Shopping");
        System.out.println("Shop from the comfort of your home!");
        System.out.println("================================================");
    }

    private boolean authenticateCustomer() {
        ui.clearScreen();
        System.out.println("=== CUSTOMER LOGIN/REGISTRATION REQUIRED ===");
        System.out.println("You must have an account to shop online with SYOS");
        System.out.println();
        System.out.println("1. Login with existing account");
        System.out.println("2. Register new account");
        System.out.println("3. Back to main menu");
        System.out.print("Select option: ");

        String choice = ui.getUserInput();

        switch (choice) {
            case "1" -> {
                return loginCustomer();
            }
            case "2" -> {
                return registerCustomer();
            }
            case "3" -> {
                return false;
            }
            default -> {
                ui.displayError("Invalid option. Please try again.");
                return authenticateCustomer();
            }
        }
    }

    private boolean loginCustomer() {
        String email = ui.getUserInput("Enter your email: ");
        String password = ui.getUserInput("Enter your password: ");

        try {
            this.currentCustomer = customerService.loginCustomer(email, password);
            ui.displaySuccess("Welcome back, " + currentCustomer.getCustomerName() + "!");
            return true;
        } catch (Exception e) {
            ui.displayError("Login failed: " + e.getMessage());
            return authenticateCustomer();
        }
    }

    private boolean registerCustomer() {
        try {
            String name = ui.getUserInput("Enter your full name: ");
            String email = ui.getUserInput("Enter your email: ");
            String password = ui.getUserInput("Enter your password (min 6 characters): ");
            String confirmPassword = ui.getUserInput("Confirm your password: ");

            if (!password.equals(confirmPassword)) {
                ui.displayError("Passwords do not match. Please try again.");
                return registerCustomer();
            }

            String phone = ui.getUserInput("Enter your phone number: ");
            String address = ui.getUserInput("Enter your address: ");

            this.currentCustomer = customerService.registerCustomer(name, email, phone, address, password);
            ui.displaySuccess("Registration successful! Welcome, " + name + "!");
            return true;
        } catch (Exception e) {
            ui.displayError("Registration failed: " + e.getMessage());
            return authenticateCustomer();
        }
    }

    private void displayOnlineMenu() {
        ui.clearScreen();
        System.out.println("================================================");
        System.out.println("           SYOS Online Store");
        System.out.println("        Welcome, " + currentCustomer.getCustomerName());
        System.out.println("================================================");
        System.out.println();
        System.out.println("=== ONLINE STORE MENU ===");
        System.out.println("1. 📂 Browse Products by Category");
        System.out.println("2. 🔍 Search Products");
        System.out.println("3. 🛒 Start Shopping");
        System.out.println("4. 📋 View My Orders");
        System.out.println("5. ⬅️  Back to Main Menu");
        System.out.println();
        System.out.print("Select option: ");
    }

    private void browseProductsByCategory() {
        try {
            ui.clearScreen();
            System.out.println("=== BROWSE BY CATEGORY ===");

            // Get categories
            Map<String, List<Product>> productsByCategory = onlineStoreService.getProductsByCategory();

            if (productsByCategory.isEmpty()) {
                ui.displayError("No products available online.");
                ui.waitForEnter();
                return;
            }

            // Display categories
            System.out.println("Available Categories:");
            System.out.println("====================");
            int index = 1;
            for (String category : productsByCategory.keySet()) {
                System.out.printf("%d. %s (%d products)\n",
                        index++, category, productsByCategory.get(category).size());
            }
            System.out.println("0. Back to menu");

            String choice = ui.getUserInput("\nSelect category: ");

            if ("0".equals(choice)) {
                return;
            }

            try {
                int categoryIndex = Integer.parseInt(choice) - 1;
                String[] categories = productsByCategory.keySet().toArray(new String[0]);

                if (categoryIndex >= 0 && categoryIndex < categories.length) {
                    String selectedCategory = categories[categoryIndex];
                    displayProductsInCategory(selectedCategory, productsByCategory.get(selectedCategory));
                } else {
                    ui.displayError("Invalid category selection");
                }
            } catch (NumberFormatException e) {
                ui.displayError("Please enter a valid number");
            }

            ui.waitForEnter();

        } catch (Exception e) {
            ui.displayError("Failed to load categories: " + e.getMessage());
            ui.waitForEnter();
        }
    }

    private void displayProductsInCategory(String category, List<Product> products) {
        ui.clearScreen();
        System.out.println("=== " + category.toUpperCase() + " ===");
        System.out.println();

        System.out.printf("%-15s %-35s %-12s %-10s %s\n",
                "Code", "Product Name", "Price", "Stock", "Description");
        System.out.println("=".repeat(90));

        for (Product product : products) {
            int onlineStock = onlineStoreService.getAvailableStock(product.getProductCode());
            String stockStatus = onlineStock > 0 ? String.valueOf(onlineStock) : "Out of Stock";

            System.out.printf("%-15s %-35s %-12s %-10s %s\n",
                    product.getProductCode().getCode(),
                    truncate(product.getProductName(), 34),
                    product.getUnitPrice(),
                    stockStatus,
                    truncate(product.getDescription() != null ? product.getDescription() : "", 30));
        }

        System.out.println("=".repeat(90));
        System.out.println("Total products in " + category + ": " + products.size());
    }

    private void searchProducts() {
        try {
            ui.clearScreen();
            String searchTerm = ui.getUserInput("Enter search term (product name, brand, category): ");

            if (searchTerm.trim().isEmpty()) {
                return;
            }

            List<Product> products = productService.searchProducts(searchTerm);

            if (products.isEmpty()) {
                ui.displayError("No products found for: " + searchTerm);
                ui.waitForEnter();
                return;
            }

            displaySearchResults(products, searchTerm);
            ui.waitForEnter();

        } catch (Exception e) {
            ui.displayError("Search failed: " + e.getMessage());
            ui.waitForEnter();
        }
    }

    private void displaySearchResults(List<Product> products, String searchTerm) {
        ui.clearScreen();
        System.out.println("=== SEARCH RESULTS FOR: " + searchTerm + " ===");
        System.out.println();

        System.out.printf("%-15s %-35s %-12s %-10s %s\n",
                "Code", "Product Name", "Price", "Stock", "Description");
        System.out.println("=".repeat(90));

        for (Product product : products) {
            int onlineStock = onlineStoreService.getAvailableStock(product.getProductCode());
            String stockStatus = onlineStock > 0 ? String.valueOf(onlineStock) : "Out of Stock";

            System.out.printf("%-15s %-35s %-12s %-10s %s\n",
                    product.getProductCode().getCode(),
                    truncate(product.getProductName(), 34),
                    product.getUnitPrice(),
                    stockStatus,
                    truncate(product.getDescription() != null ? product.getDescription() : "", 30));
        }

        System.out.println("=".repeat(90));
        System.out.println("Found " + products.size() + " products");
    }

    private void startShopping() {
        try {
            ui.clearScreen();
            ui.displaySuccess("Starting online shopping session...");

            // Create new online bill (customer is guaranteed to exist now)
            Bill bill = billingService.createNewOnlineBill(currentCustomer, LocalDate.now());

            // Shopping phase
            boolean addingItems = true;
            while (addingItems) {
                ProductCode productCode = null;
                try {
                    productCode = ui.getProductCode();

                    if (productCode == null) {
                        // User typed 'done'
                        addingItems = false;
                        continue;
                    }

                    // Check if product is available online
                    int availableStock = onlineStoreService.getAvailableStock(productCode);
                    if (availableStock == 0) {
                        ui.displayError("Product not available online: " + productCode);
                        continue;
                    }

                    // Show product details
                    Product product = productService.findProductByCode(productCode);
                    System.out.println("\nProduct: " + product.getProductName());
                    System.out.println("Price: " + product.getUnitPrice());
                    System.out.println("Available: " + availableStock + " units");
                    System.out.println("Description: " + (product.getDescription() != null ? product.getDescription() : "N/A"));

                    // Get quantity
                    int quantity = ui.getQuantity();

                    if (quantity > availableStock) {
                        ui.displayInsufficientStock(product.getProductName(), availableStock, quantity);
                        continue;
                    }

                    // Add item to bill
                    BillItem item = billingService.addItemToOnlineBill(bill, productCode, quantity);

                    // Display added item and running total
                    ui.displayBillItem(item);
                    ui.displayRunningTotal(billingService.calculateRunningTotal(bill));

                } catch (ProductNotFoundException e) {
                    ui.displayError("Product not found: " + e.getMessage());
                } catch (InsufficientStockException e) {
                    ui.displayInsufficientStock(
                            productCode.getCode(),
                            e.getAvailableStock(),
                            e.getRequestedQuantity()
                    );
                } catch (BillingException e) {
                    ui.displayError("Order error: " + e.getMessage());
                }
            }

            // Check if bill is empty
            if (bill.isEmpty()) {
                ui.displayError("No items added to order. Shopping cancelled.");
                return;
            }

            // Display order summary
            ui.displayBillSummary(bill);

            // Confirm order
            if (!ui.confirmAction("Confirm order (Cash on Delivery)?")) {
                ui.displaySuccess("Order cancelled.");
                return;
            }

            // Process order (Cash on Delivery - no payment needed now)
            Bill savedBill = billingService.saveOnlineBill(bill);

            // Display order confirmation
            displayOrderConfirmation(savedBill);

            ui.waitForEnter();

        } catch (Exception e) {
            ui.displayError("Shopping failed: " + e.getMessage());
        }
    }

    private void displayOrderConfirmation(Bill bill) {
        ui.clearScreen();
        System.out.println("=".repeat(50));
        System.out.println("           ORDER CONFIRMATION");
        System.out.println("            SYOS Online Store");
        System.out.println("=".repeat(50));
        System.out.printf("Order No: %s\n", bill.getBillSerialNumber());
        System.out.printf("Date: %s\n", bill.getBillDate());
        System.out.printf("Customer: %s\n", currentCustomer.getCustomerName());
        System.out.printf("Payment: Cash on Delivery\n");
        System.out.println("-".repeat(50));

        for (BillItem item : bill.getItems()) {
            System.out.printf("%-25s %2dx %8s = %10s\n",
                    item.getProductName(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getTotalPrice());
        }

        System.out.println("-".repeat(50));
        System.out.printf("Total Amount: %s\n", bill.getTotalAmount());
        System.out.println("=".repeat(50));
        System.out.println("🚚 Your order will be delivered soon!");
        System.out.println("💰 Payment: Cash on Delivery");
        System.out.println("📧 Confirmation sent to your email");
        System.out.println("=".repeat(50));
    }

    private void viewMyOrders() {
        // Customer is guaranteed to exist (no guest users allowed)
        try {
            List<Bill> orders = billingService.getCustomerOrders(currentCustomer.getCustomerId());

            if (orders.isEmpty()) {
                ui.displayError("No orders found.");
                ui.waitForEnter();
                return;
            }

            displayCustomerOrders(orders);
            ui.waitForEnter();

        } catch (Exception e) {
            ui.displayError("Failed to load orders: " + e.getMessage());
            ui.waitForEnter();
        }
    }

    private void displayCustomerOrders(List<Bill> orders) {
        ui.clearScreen();
        System.out.println("=== MY ORDERS ===");
        System.out.println();

        System.out.printf("%-15s %-12s %-15s %-12s %s\n",
                "Order No", "Date", "Items", "Total", "Status");
        System.out.println("=".repeat(70));

        for (Bill order : orders) {
            System.out.printf("%-15s %-12s %-15d %-12s %s\n",
                    order.getBillSerialNumber().getSerialNumber(),
                    order.getBillDate(),
                    order.getItemCount(),
                    order.getTotalAmount(),
                    "Confirmed");
        }

        System.out.println("=".repeat(70));
        System.out.println("Total orders: " + orders.size());
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}