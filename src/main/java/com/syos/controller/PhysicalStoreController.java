package com.syos.controller;

import com.syos.domain.models.Bill;
import com.syos.domain.models.BillItem;
import com.syos.domain.models.Product;
import com.syos.domain.valueobjects.ProductCode;
import com.syos.domain.valueobjects.Money;
import com.syos.domain.enums.StoreType;
import com.syos.exceptions.ProductNotFoundException;
import com.syos.exceptions.InsufficientStockException;
import com.syos.exceptions.InvalidPaymentException;
import com.syos.exceptions.BillingException;
import com.syos.service.interfaces.BillingService;
import com.syos.service.interfaces.ProductService;
import com.syos.ui.interfaces.UserInterface;
import java.time.LocalDate;
import java.util.List;

public class PhysicalStoreController {
    private final BillingService billingService;
    private final ProductService productService;
    private final UserInterface ui;

    public PhysicalStoreController(BillingService billingService,
                                   ProductService productService,
                                   UserInterface ui) {
        this.billingService = billingService;
        this.productService = productService;
        this.ui = ui;
    }

    public void startCashierMode() {
        ui.clearScreen();
        System.out.println("================================================");
        System.out.println("           SYOS - Physical Store POS");
        System.out.println("================================================");
        System.out.println("Welcome to SYOS Point of Sale System");
        System.out.println("Physical Store - Cash Only");
        System.out.println("================================================");

        boolean running = true;
        while (running) {
            try {
                ui.displayMenu();
                String choice = ui.getUserInput();

                switch (choice) {
                    case "1" -> startNewTransaction();
                    case "2" -> searchProducts();
                    case "3" -> viewReports();
                    case "4" -> {
                        ui.displaySuccess("Returning to main menu...");
                        running = false;
                    }
                    default -> ui.displayError("Invalid option. Please try again.");
                }
            } catch (Exception e) {
                ui.displayError("Unexpected error: " + e.getMessage());
                e.printStackTrace(); // For debugging
            }
        }
    }

    private void startNewTransaction() {
        try {
            ui.clearScreen();
            ui.displaySuccess("Starting new transaction...");

            // Create new bill
            Bill bill = billingService.createNewBill(StoreType.PHYSICAL, LocalDate.now());

            // Item entry phase
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

                    // Get quantity
                    int quantity = ui.getQuantity();

                    // Add item to bill
                    BillItem item = billingService.addItemToBill(bill, productCode, quantity);

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
                    ui.displayError("Billing error: " + e.getMessage());
                }
            }

            // Check if bill is empty
            if (bill.isEmpty()) {
                ui.displayError("No items added to bill. Transaction cancelled.");
                return;
            }

            // Display bill summary
            ui.displayBillSummary(bill);

            // Confirm transaction
            if (!ui.confirmAction("Proceed with payment?")) {
                ui.displaySuccess("Transaction cancelled.");
                return;
            }

            // Payment phase
            processPayment(bill);

        } catch (Exception e) {
            ui.displayError("Transaction failed: " + e.getMessage());
        }
    }

    private void processPayment(Bill bill) {
        boolean paymentComplete = false;

        while (!paymentComplete) {
            try {
                ui.displaySuccess(String.format("Total amount: %s", bill.getTotalAmount()));
                Money cashTendered = ui.getCashAmount();

                // Complete the bill (includes payment processing and stock reduction)
                billingService.completeBill(bill, cashTendered);

                // Save bill to database
                Bill savedBill = billingService.saveBill(bill);

                // Display receipt
                ui.displayReceipt(savedBill);

                ui.displaySuccess("Transaction completed successfully!");
                ui.displaySuccess(String.format("Bill saved with number: %s", savedBill.getBillSerialNumber()));

                paymentComplete = true;
                ui.waitForEnter();

            } catch (InvalidPaymentException e) {
                ui.displayError("Payment error: " + e.getMessage());
            } catch (BillingException e) {
                ui.displayError("Failed to complete transaction: " + e.getMessage());
                break;
            }
        }
    }

    private void searchProducts() {
        try {
            ui.clearScreen();
            String searchTerm = ui.getUserInput("Enter search term (product code, name, category, brand) or press Enter for all: ");

            List<Product> products = productService.searchProducts(searchTerm);
            ui.displayProductSearch(products);

            ui.waitForEnter();

        } catch (Exception e) {
            ui.displayError("Search failed: " + e.getMessage());
        }
    }

    private void viewReports() {
        ui.displayError("Reports feature not yet implemented.");
        ui.waitForEnter();
    }
}