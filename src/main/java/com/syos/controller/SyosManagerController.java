package com.syos.controller;

import com.syos.service.interfaces.ReportService;
import com.syos.service.interfaces.ReportService.*;
import com.syos.ui.interfaces.UserInterface;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Controller for SYOS Manager operations.
 * Provides CLI interface for generating and viewing various reports.
 * Follows MVC pattern - handles user interactions and delegates to services.
 */
public class SyosManagerController {

    private final ReportService reportService;
    private final UserInterface ui;

    public SyosManagerController(ReportService reportService, UserInterface ui) {
        this.reportService = reportService;
        this.ui = ui;
    }

    public void startManagerMode() {
        ui.clearScreen();
        displayWelcome();

        boolean running = true;
        while (running) {
            try {
                displayMainMenu();
                String choice = ui.getUserInput();

                switch (choice) {
                    case "1" -> generateDailySalesReport();
                    case "2" -> generateRestockReport();
                    case "3" -> generateReorderReport();
                    case "4" -> generateStockReport();
                    case "5" -> generateBillReport();
                    case "6" -> {
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
        System.out.println("           SYOS - Manager Dashboard");
        System.out.println("================================================");
        System.out.println("Generate comprehensive reports for business insights");
        System.out.println("================================================");
    }

    private void displayMainMenu() {
        ui.clearScreen();
        System.out.println("================================================");
        System.out.println("           SYOS - Manager Dashboard");
        System.out.println("================================================");
        System.out.println();
        System.out.println("=== REPORTS MENU ===");
        System.out.println("1. ЁЯУК Daily Sales Report");
        System.out.println("2. ЁЯУж Restock Report (Maintain 70 units)");
        System.out.println("3. тЪая╕П  Reorder Report (Below 50 units)");
        System.out.println("4. ЁЯУЛ Stock Report (Batch-wise details)");
        System.out.println("5. ЁЯз╛ Bill Report (All transactions)");
        System.out.println("6. тмЕя╕П Back to Main Menu");
        System.out.println();
        System.out.print("Select report to generate: ");
    }

    // ==================== REPORT GENERATION METHODS ====================

    private void generateDailySalesReport() {
        ui.clearScreen();
        System.out.println("=== DAILY SALES REPORT ===");

        LocalDate reportDate = getReportDate("Enter date for sales report");
        if (reportDate == null) return;

        try {
            DailySalesReport report = reportService.generateDailySalesReport(reportDate);
            displayDailySalesReport(report);
        } catch (Exception e) {
            ui.displayError("Failed to generate sales report: " + e.getMessage());
        }

        ui.waitForEnter();
    }

    private void generateRestockReport() {
        ui.clearScreen();
        System.out.println("=== RESTOCK REPORT ===");
        System.out.println("Shows items needed to maintain 70 units in each store");

        LocalDate reportDate = getReportDate("Enter date to check inventory levels");
        if (reportDate == null) return;

        try {
            RestockReport report = reportService.generateRestockReport(reportDate);
            displayRestockReport(report);
        } catch (Exception e) {
            ui.displayError("Failed to generate restock report: " + e.getMessage());
        }

        ui.waitForEnter();
    }

    private void generateReorderReport() {
        ui.clearScreen();
        System.out.println("=== REORDER REPORT ===");
        System.out.println("Shows main inventory items below 50 units");

        LocalDate reportDate = getReportDate("Enter date to check stock levels");
        if (reportDate == null) return;

        try {
            ReorderReport report = reportService.generateReorderReport(reportDate);
            displayReorderReport(report);
        } catch (Exception e) {
            ui.displayError("Failed to generate reorder report: " + e.getMessage());
        }

        ui.waitForEnter();
    }

    private void generateStockReport() {
        ui.clearScreen();
        System.out.println("=== STOCK REPORT ===");
        System.out.println("Shows batch-wise stock details across all inventories");

        LocalDate reportDate = getReportDate("Enter date to get stock details up to");
        if (reportDate == null) return;

        try {
            StockReport report = reportService.generateStockReport(reportDate);
            displayStockReport(report);
        } catch (Exception e) {
            ui.displayError("Failed to generate stock report: " + e.getMessage());
        }

        ui.waitForEnter();
    }

    private void generateBillReport() {
        ui.clearScreen();
        System.out.println("=== BILL REPORT ===");
        System.out.println("Shows all customer transactions for a specific date");

        LocalDate reportDate = getReportDate("Enter date to get bill transactions");
        if (reportDate == null) return;

        try {
            BillReport report = reportService.generateBillReport(reportDate);
            displayBillReport(report);
        } catch (Exception e) {
            ui.displayError("Failed to generate bill report: " + e.getMessage());
        }

        ui.waitForEnter();
    }

    // ==================== REPORT DISPLAY METHODS ====================

    private void displayDailySalesReport(DailySalesReport report) {
        ui.clearScreen();
        System.out.println("=".repeat(80));
        System.out.println("                           DAILY SALES REPORT");
        System.out.println("                              " + report.getReportDate());
        System.out.println("=".repeat(80));

        // Physical Store Section
        System.out.println("\nЁЯУН PHYSICAL STORE SALES");
        System.out.println("-".repeat(80));
        PhysicalStoreSales physicalSales = report.getPhysicalStoreSales();

        if (physicalSales.getItems().isEmpty()) {
            System.out.println("No physical store sales for this date.");
        } else {
            System.out.printf("%-15s %-35s %-10s %-15s\n", "Code", "Product Name", "Quantity", "Revenue");
            System.out.println("-".repeat(80));

            for (SalesItem item : physicalSales.getItems()) {
                System.out.printf("%-15s %-35s %-10d LKR %,12.2f\n",
                        item.getProductCode(),
                        truncateString(item.getProductName(), 34),
                        item.getTotalQuantity(),
                        item.getRevenue());
            }
        }

        System.out.println("-".repeat(80));
        System.out.printf("PHYSICAL STORE TOTAL: LKR %,15.2f\n", physicalSales.getRevenue());

        // Online Store Section
        System.out.println("\nЁЯМР ONLINE STORE SALES");
        System.out.println("-".repeat(80));
        OnlineStoreSales onlineSales = report.getOnlineStoreSales();

        if (onlineSales.getItems().isEmpty()) {
            System.out.println("No online store sales for this date.");
        } else {
            System.out.printf("%-15s %-35s %-10s %-15s\n", "Code", "Product Name", "Quantity", "Revenue");
            System.out.println("-".repeat(80));

            for (SalesItem item : onlineSales.getItems()) {
                System.out.printf("%-15s %-35s %-10d LKR %,12.2f\n",
                        item.getProductCode(),
                        truncateString(item.getProductName(), 34),
                        item.getTotalQuantity(),
                        item.getRevenue());
            }
        }

        System.out.println("-".repeat(80));
        System.out.printf("ONLINE STORE TOTAL:   LKR %,15.2f\n", onlineSales.getRevenue());

        // Grand Total
        System.out.println("=".repeat(80));
        System.out.printf("GRAND TOTAL REVENUE:  LKR %,15.2f\n", report.getTotalRevenue());
        System.out.println("=".repeat(80));
    }

    private void displayRestockReport(RestockReport report) {
        ui.clearScreen();
        System.out.println("=".repeat(80));
        System.out.println("                            RESTOCK REPORT");
        System.out.println("                              " + report.getReportDate());
        System.out.println("=".repeat(80));

        // Physical Store Section
        System.out.println("\nЁЯУН PHYSICAL STORE RESTOCK NEEDS");
        System.out.println("-".repeat(80));
        List<RestockItem> physicalItems = report.getPhysicalStoreItems();

        if (physicalItems.isEmpty()) {
            System.out.println("тЬЕ All physical store items are adequately stocked (70+ units).");
        } else {
            System.out.printf("%-15s %-35s %-10s %-10s\n", "Code", "Product Name", "Current", "Needed");
            System.out.println("-".repeat(80));

            for (RestockItem item : physicalItems) {
                System.out.printf("%-15s %-35s %-10d %-10d\n",
                        item.getProductCode(),
                        truncateString(item.getProductName(), 34),
                        item.getCurrentQuantity(),
                        item.getQuantityNeeded());
            }

            System.out.println("-".repeat(80));
            System.out.printf("Total items needing restock: %d\n", physicalItems.size());
        }

        // Online Store Section
        System.out.println("\nЁЯМР ONLINE STORE RESTOCK NEEDS");
        System.out.println("-".repeat(80));
        List<RestockItem> onlineItems = report.getOnlineStoreItems();

        if (onlineItems.isEmpty()) {
            System.out.println("тЬЕ All online store items are adequately stocked (70+ units).");
        } else {
            System.out.printf("%-15s %-35s %-10s %-10s\n", "Code", "Product Name", "Current", "Needed");
            System.out.println("-".repeat(80));

            for (RestockItem item : onlineItems) {
                System.out.printf("%-15s %-35s %-10d %-10d\n",
                        item.getProductCode(),
                        truncateString(item.getProductName(), 34),
                        item.getCurrentQuantity(),
                        item.getQuantityNeeded());
            }

            System.out.println("-".repeat(80));
            System.out.printf("Total items needing restock: %d\n", onlineItems.size());
        }

        System.out.println("=".repeat(80));
    }

    private void displayReorderReport(ReorderReport report) {
        ui.clearScreen();
        System.out.println("=".repeat(80));
        System.out.println("                            REORDER REPORT");
        System.out.println("                        Main Inventory - " + report.getReportDate());
        System.out.println("=".repeat(80));

        List<ReorderItem> items = report.getItems();

        if (items.isEmpty()) {
            System.out.println("тЬЕ All products in main inventory have adequate stock (50+ units).");
        } else {
            System.out.printf("%-15s %-40s %-10s %-10s\n", "Code", "Product Name", "Available", "Status");
            System.out.println("-".repeat(80));

            for (ReorderItem item : items) {
                String statusIcon = item.getStatus().equals("CRITICAL") ? "ЁЯЪи" : "тЪая╕П";

                System.out.printf("%-15s %-40s %-10d %-10s %s\n",
                        item.getProductCode(),
                        truncateString(item.getProductName(), 39),
                        item.getTotalQuantityAvailable(),
                        item.getStatus(),
                        statusIcon);
            }

            System.out.println("-".repeat(80));

            // Summary by status
            long criticalCount = items.stream().filter(item -> "CRITICAL".equals(item.getStatus())).count();
            long lowCount = items.stream().filter(item -> "LOW".equals(item.getStatus())).count();

            System.out.printf("CRITICAL items (0 units): %d\n", criticalCount);
            System.out.printf("LOW items (1-49 units): %d\n", lowCount);
            System.out.printf("Total items needing reorder: %d\n", items.size());
        }

        System.out.println("=".repeat(80));
    }

    private void displayStockReport(StockReport report) {
        ui.clearScreen();
        System.out.println("=".repeat(120));
        System.out.println("                                           STOCK REPORT - BATCH WISE");
        System.out.println("                                              " + report.getReportDate());
        System.out.println("=".repeat(120));

        List<StockItem> items = report.getStockItems();

        if (items.isEmpty()) {
            System.out.println("No stock data available for the specified date.");
        } else {
            System.out.printf("%-12s %-25s %-8s %-8s %-12s %-12s %-8s %-8s %-8s %-8s\n",
                    "Code", "Product", "Batch", "Received", "Purchase", "Expiry", "Main", "Phys", "Online", "Total");
            System.out.println("-".repeat(120));

            for (StockItem item : items) {
                System.out.printf("%-12s %-25s %-8s %-8d %-12s %-12s %-8d %-8d %-8d %-8d\n",
                        item.getProductCode(),
                        truncateString(item.getProductName(), 24),
                        item.getBatchNumber() > 0 ? String.valueOf(item.getBatchNumber()) : "N/A",
                        item.getQuantityReceived(),
                        item.getPurchaseDate() != null ? item.getPurchaseDate().toString() : "N/A",
                        item.getExpiryDate() != null ? item.getExpiryDate().toString() : "No expiry",
                        item.getMainInventoryRemaining(),
                        item.getPhysicalStoreQuantity(),
                        item.getOnlineStoreQuantity(),
                        item.getTotalQuantity());
            }

            System.out.println("-".repeat(120));

            // Calculate totals
            int totalMain = items.stream().mapToInt(StockItem::getMainInventoryRemaining).sum();
            int totalPhysical = items.stream().mapToInt(StockItem::getPhysicalStoreQuantity).sum();
            int totalOnline = items.stream().mapToInt(StockItem::getOnlineStoreQuantity).sum();
            int grandTotal = items.stream().mapToInt(StockItem::getTotalQuantity).sum();

            System.out.printf("%-12s %-25s %-8s %-8s %-12s %-12s %-8d %-8d %-8d %-8d\n",
                    "TOTALS", "", "", "", "", "", totalMain, totalPhysical, totalOnline, grandTotal);
        }

        System.out.println("=".repeat(120));
    }

    private void displayBillReport(BillReport report) {
        ui.clearScreen();
        System.out.println("=".repeat(90));
        System.out.println("                                    BILL REPORT");
        System.out.println("                                     " + report.getReportDate());
        System.out.println("=".repeat(90));

        // Physical Store Bills
        System.out.println("\nЁЯУН PHYSICAL STORE TRANSACTIONS");
        System.out.println("-".repeat(90));
        List<BillSummary> physicalBills = report.getPhysicalStoreBills();

        if (physicalBills.isEmpty()) {
            System.out.println("No physical store transactions for this date.");
        } else {
            System.out.printf("%-15s %-12s %-20s %-8s %-15s %-12s\n",
                    "Bill Number", "Date", "Customer", "Items", "Amount", "Type");
            System.out.println("-".repeat(90));

            for (BillSummary bill : physicalBills) {
                System.out.printf("%-15s %-12s %-20s %-8d LKR %,10.2f %-12s\n",
                        bill.getBillSerialNumber(),
                        bill.getBillDate(),
                        bill.getCustomerName() != null ? truncateString(bill.getCustomerName(), 19) : "Walk-in",
                        bill.getItemCount(),
                        bill.getTotalAmount(),
                        bill.getTransactionType());
            }
        }

        System.out.println("-".repeat(90));
        System.out.printf("Physical Store Transactions: %d\n", report.getTotalPhysicalTransactions());

        // Online Store Bills
        System.out.println("\nЁЯМР ONLINE STORE TRANSACTIONS");
        System.out.println("-".repeat(90));
        List<BillSummary> onlineBills = report.getOnlineStoreBills();

        if (onlineBills.isEmpty()) {
            System.out.println("No online store transactions for this date.");
        } else {
            System.out.printf("%-15s %-12s %-20s %-8s %-15s %-12s\n",
                    "Bill Number", "Date", "Customer", "Items", "Amount", "Type");
            System.out.println("-".repeat(90));

            for (BillSummary bill : onlineBills) {
                System.out.printf("%-15s %-12s %-20s %-8d LKR %,10.2f %-12s\n",
                        bill.getBillSerialNumber(),
                        bill.getBillDate(),
                        bill.getCustomerName() != null ? truncateString(bill.getCustomerName(), 19) : "Guest",
                        bill.getItemCount(),
                        bill.getTotalAmount(),
                        bill.getTransactionType());
            }
        }

        System.out.println("-".repeat(90));
        System.out.printf("Online Store Transactions: %d\n", report.getTotalOnlineTransactions());

        // Summary
        System.out.println("=".repeat(90));
        System.out.printf("TOTAL TRANSACTIONS: %d\n", report.getTotalTransactions());
        System.out.println("=".repeat(90));
    }

    // ==================== HELPER METHODS ====================

    private LocalDate getReportDate(String prompt) {
        while (true) {
            try {
                String input = ui.getUserInput(prompt + " (YYYY-MM-DD) or 'today': ");

                if ("today".equalsIgnoreCase(input.trim())) {
                    return LocalDate.now();
                }

                if (input.trim().isEmpty()) {
                    ui.displayError("Date cannot be empty. Please try again.");
                    continue;
                }

                LocalDate date = LocalDate.parse(input.trim());

                // Validate date is not in the future
                if (date.isAfter(LocalDate.now())) {
                    ui.displayError("Date cannot be in the future. Please try again.");
                    continue;
                }

                return date;

            } catch (DateTimeParseException e) {
                ui.displayError("Invalid date format. Please use YYYY-MM-DD format.");
            } catch (Exception e) {
                ui.displayError("Error processing date: " + e.getMessage());

                // Ask if user wants to cancel
                if (!ui.confirmAction("Try again?")) {
                    return null;
                }
            }
        }
    }

    private String truncateString(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }
}