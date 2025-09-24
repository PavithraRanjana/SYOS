package com.syos;

import com.syos.controller.MainMenuController;
import com.syos.repository.impl.*;
import com.syos.service.impl.*;
import com.syos.ui.impl.ConsoleUserInterface;
import com.syos.utils.DatabaseConnection;

/**
 * Updated SYOS Application with complete functionality including Report Management.
 *
 * This class initializes all dependencies and starts the application.
 * Now includes the complete system with:
 * - Product management with auto-generated codes
 * - Batch management with FIFO+Expiry strategy
 * - Stock issuing with detailed analysis
 * - Undo functionality using Command pattern
 * - Comprehensive reporting for SYOS Manager
 */
public class SyosApplication {
    public static void main(String[] args) {
        try {
            // Display startup banner
            displayStartupBanner();

            // Test database connection first
            System.out.println("🚀 Initializing SYOS Application...");
            DatabaseConnection.getInstance().testConnection();

            // Initialize repositories
            System.out.println("📚 Initializing repositories...");
            ProductRepositoryImpl productRepository = new ProductRepositoryImpl();
            InventoryRepositoryImpl inventoryRepository = new InventoryRepositoryImpl();
            BillRepositoryImpl billRepository = new BillRepositoryImpl();
            CustomerRepositoryImpl customerRepository = new CustomerRepositoryImpl();
            ReportRepositoryImpl reportRepository = new ReportRepositoryImpl(); // Added ReportRepository

            // Initialize core services
            System.out.println("⚙️ Initializing core services...");
            ProductServiceImpl productService = new ProductServiceImpl(productRepository, inventoryRepository);
            InventoryServiceImpl inventoryService = new InventoryServiceImpl(inventoryRepository);
            CashPaymentServiceImpl paymentService = new CashPaymentServiceImpl();
            CustomerServiceImpl customerService = new CustomerServiceImpl(customerRepository);
            OnlineStoreServiceImpl onlineStoreService = new OnlineStoreServiceImpl(productRepository, inventoryRepository);

            // Initialize billing service
            BillingServiceImpl billingService = new BillingServiceImpl(
                    productService, inventoryService, paymentService, billRepository);

            // Initialize inventory manager components
            System.out.println("📦 Initializing inventory management system...");
            ProductCodeGeneratorImpl codeGenerator = new ProductCodeGeneratorImpl();

            InventoryManagerServiceImpl inventoryManagerService = new InventoryManagerServiceImpl(
                    productRepository, inventoryRepository, codeGenerator);

            // Initialize report service
            System.out.println("📊 Initializing report management system...");
            ReportServiceImpl reportService = new ReportServiceImpl(reportRepository);

            // Initialize UI
            System.out.println("🖥️ Initializing user interface...");
            ConsoleUserInterface ui = new ConsoleUserInterface();

            // Initialize main controller with all services including ReportService
            MainMenuController mainController = new MainMenuController(
                    billingService,
                    productService,
                    onlineStoreService,
                    customerService,
                    inventoryManagerService,
                    reportService,  // Added ReportService
                    ui);

            // Display system ready message
            displaySystemReady();

            // Start application
            mainController.start();

            // Cleanup
            ui.close();

            System.out.println("\n👋 Thank you for using SYOS!");
            System.out.println("Application terminated successfully.");

        } catch (Exception e) {
            System.err.println("❌ Failed to start SYOS application: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void displayStartupBanner() {
        System.out.println();
        System.out.println("╔═════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                             ║");
        System.out.println("║   ███████╗██╗   ██╗ ██████╗ ███████╗                      ║");
        System.out.println("║   ██╔════╝╚██╗ ██╔╝██╔═══██╗██╔════╝                      ║");
        System.out.println("║   ███████╗ ╚████╔╝ ██║   ██║███████╗                      ║");
        System.out.println("║   ╚════██║  ╚██╔╝  ██║   ██║╚════██║                      ║");
        System.out.println("║   ███████║   ██║   ╚██████╔╝███████║                      ║");
        System.out.println("║   ╚══════╝   ╚═╝    ╚═════╝ ╚══════╝                      ║");
        System.out.println("║                                                             ║");
        System.out.println("║              Synex Outlet Store (SYOS)                     ║");
        System.out.println("║           Complete Retail Management System                ║");
        System.out.println("║                      Version 2.0                           ║");
        System.out.println("║                                                             ║");
        System.out.println("╚═════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println();
    }

    private static void displaySystemReady() {
        System.out.println("✅ All systems initialized successfully!");
        System.out.println();
        System.out.println("🎉 SYOS is ready for use!");
        System.out.println();
        System.out.println("📋 Available Roles:");
        System.out.println("   1. 💳 Cashier - Physical store point of sale");
        System.out.println("   2. 📦 Inventory Manager - Product & stock management");
        System.out.println("   3. 📊 SYOS Manager - Business reports & analytics");
        System.out.println("   4. 🛒 Online Customer - E-commerce shopping");
        System.out.println();
        System.out.println("🚀 Starting main menu...");
        System.out.println("=".repeat(60));
        System.out.println();

        // Small delay for better UX
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}