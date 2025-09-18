package com.syos.ui.impl;

import com.syos.domain.models.Bill;
import com.syos.domain.models.BillItem;
import com.syos.domain.models.Product;
import com.syos.domain.valueobjects.Money;
import com.syos.domain.valueobjects.ProductCode;
import com.syos.ui.interfaces.UserInterface;
import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;

public class ConsoleUserInterface implements UserInterface {
    private final Scanner scanner;
    private static final String SEPARATOR = "================================================";
    private static final String HEADER = "           SYOS - Physical Store POS";

    public ConsoleUserInterface() {
        this.scanner = new Scanner(System.in);
    }

    @Override
    public void displayWelcome() {
        clearScreen();
        System.out.println(SEPARATOR);
        System.out.println(HEADER);
        System.out.println(SEPARATOR);
        System.out.println("Welcome to SYOS Point of Sale System");
        System.out.println("Physical Store - Cash Only");
        System.out.println(SEPARATOR);
    }

    @Override
    public void displayMenu() {
        System.out.println("\n=== MAIN MENU ===");
        System.out.println("1. Start New Transaction");
        System.out.println("2. Search Products");
        System.out.println("3. View Reports");
        System.out.println("4. Exit");
        System.out.print("Select option: ");
    }

    @Override
    public String getUserInput() {
        return scanner.nextLine().trim();
    }

    @Override
    public String getUserInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    @Override
    public ProductCode getProductCode() {
        while (true) {
            try {
                String input = getUserInput("Enter product code (or 'done' to finish): ");
                if ("done".equalsIgnoreCase(input)) {
                    return null; // Signal completion
                }
                return new ProductCode(input);
            } catch (IllegalArgumentException e) {
                displayError("Invalid product code: " + e.getMessage());
            }
        }
    }

    @Override
    public int getQuantity() {
        while (true) {
            try {
                String input = getUserInput("Enter quantity: ");
                int quantity = Integer.parseInt(input);
                if (quantity <= 0) {
                    displayError("Quantity must be positive");
                    continue;
                }
                return quantity;
            } catch (NumberFormatException e) {
                displayError("Invalid quantity. Please enter a number.");
            }
        }
    }

    @Override
    public Money getCashAmount() {
        while (true) {
            try {
                String input = getUserInput("Enter cash amount: LKR ");
                BigDecimal amount = new BigDecimal(input);
                return new Money(amount);
            } catch (Exception e) {
                displayError("Invalid amount. Please enter a valid number.");
            }
        }
    }

    @Override
    public void displayProduct(Product product) {
        System.out.printf("Product: %s - %s (%s)\n",
                product.getProductCode(),
                product.getProductName(),
                product.getUnitPrice());
    }

    @Override
    public void displayBillItem(BillItem item) {
        System.out.printf("+ %s x%d = %s\n",
                item.getProductName(),
                item.getQuantity(),
                item.getTotalPrice());
    }

    @Override
    public void displayRunningTotal(Money total) {
        System.out.println("─".repeat(40));
        System.out.printf("Running Total: %s\n", total);
        System.out.println("─".repeat(40));
    }

    @Override
    public void displayBillSummary(Bill bill) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("                BILL SUMMARY");
        System.out.println("=".repeat(50));

        for (BillItem item : bill.getItems()) {
            System.out.printf("%-25s %2dx %8s = %10s\n",
                    item.getProductName(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getTotalPrice());
        }

        System.out.println("-".repeat(50));
        System.out.printf("Subtotal:%40s\n", bill.getSubtotal());
        System.out.printf("Discount:%40s\n", bill.getDiscountAmount());
        System.out.printf("TOTAL:%43s\n", bill.getTotalAmount());
        System.out.println("=".repeat(50));
    }

    @Override
    public void displayReceipt(Bill bill) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("                   RECEIPT");
        System.out.println("            SYOS Grocery Store");
        System.out.println("=".repeat(50));
        System.out.printf("Bill No: %s\n", bill.getBillSerialNumber());
        System.out.printf("Date: %s\n", bill.getBillDate());
        System.out.printf("Store: %s\n", bill.getStoreType());
        System.out.println("-".repeat(50));

        for (BillItem item : bill.getItems()) {
            System.out.printf("%-25s %2dx %8s = %10s\n",
                    item.getProductName(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getTotalPrice());
        }

        System.out.println("-".repeat(50));
        System.out.printf("Subtotal:%40s\n", bill.getSubtotal());
        System.out.printf("Discount:%40s\n", bill.getDiscountAmount());
        System.out.printf("TOTAL:%43s\n", bill.getTotalAmount());
        System.out.println("-".repeat(50));
        System.out.printf("Cash Tendered:%32s\n", bill.getCashTendered());
        System.out.printf("Change:%42s\n", bill.getChangeAmount());
        System.out.println("=".repeat(50));
        System.out.println("      Thank you for shopping with SYOS!");
        System.out.println("=".repeat(50));
    }

    @Override
    public void displayError(String message) {
        System.err.println("ERROR: " + message);
    }

    @Override
    public void displaySuccess(String message) {
        System.out.println("SUCCESS: " + message);
    }

    @Override
    public void displayInsufficientStock(String productName, int available, int requested) {
        System.err.printf("INSUFFICIENT STOCK: %s\n", productName);
        System.err.printf("Available: %d, Requested: %d\n", available, requested);
    }

    @Override
    public void displayProductSearch(List<Product> products) {
        if (products.isEmpty()) {
            System.out.println("No products found.");
            return;
        }

        System.out.println("\n=== PRODUCT SEARCH RESULTS ===");
        System.out.printf("%-15s %-30s %-10s\n", "Code", "Name", "Price");
        System.out.println("-".repeat(60));

        for (Product product : products) {
            System.out.printf("%-15s %-30s %s\n",
                    product.getProductCode(),
                    product.getProductName(),
                    product.getUnitPrice());
        }
        System.out.println("-".repeat(60));
        System.out.printf("Total products found: %d\n", products.size());
    }

    @Override
    public boolean confirmAction(String message) {
        String response = getUserInput(message + " (y/n): ");
        return "y".equalsIgnoreCase(response) || "yes".equalsIgnoreCase(response);
    }

    @Override
    public void clearScreen() {
        // Simple clear screen - works on most terminals
        System.out.print("\033[2J\033[H");
        System.out.flush();
    }

    @Override
    public void waitForEnter() {
        getUserInput("Press Enter to continue...");
    }

    public void close() {
        scanner.close();
    }
}
