package com.syos.controller;

import com.syos.service.impl.BillingServiceImpl;
import com.syos.service.interfaces.InventoryManagerService;
import com.syos.service.interfaces.ReportService;
import com.syos.ui.interfaces.UserInterface;
import com.syos.service.interfaces.BillingService;
import com.syos.service.interfaces.ProductService;
import com.syos.service.interfaces.OnlineStoreService;
import com.syos.service.interfaces.CustomerService;

public class MainMenuController {
    private final BillingService billingService;
    private final ProductService productService;
    private final OnlineStoreService onlineStoreService;
    private final CustomerService customerService;
    private final InventoryManagerService inventoryManagerService;
    private final ReportService reportService; // Added ReportService
    private final UserInterface ui;

    private final PhysicalStoreController cashierController;
    private final OnlineCustomerController onlineCustomerController;
    private final InventoryManagerController inventoryManagerController;
    private final SyosManagerController syosManagerController; // Added SyosManagerController

    public MainMenuController(BillingServiceImpl billingService,
                              ProductService productService,
                              OnlineStoreService onlineStoreService,
                              CustomerService customerService,
                              InventoryManagerService inventoryManagerService,
                              ReportService reportService, // Added ReportService parameter
                              UserInterface ui) {
        this.billingService = billingService;
        this.productService = productService;
        this.onlineStoreService = onlineStoreService;
        this.customerService = customerService;
        this.inventoryManagerService = inventoryManagerService;
        this.reportService = reportService; // Initialize ReportService
        this.ui = ui;

        // Initialize sub-controllers
        this.cashierController = new PhysicalStoreController(billingService, productService, ui);
        this.onlineCustomerController = new OnlineCustomerController(billingService, productService,
                onlineStoreService, customerService, ui);
        this.inventoryManagerController = new InventoryManagerController(inventoryManagerService, ui);
        this.syosManagerController = new SyosManagerController(reportService, ui); // Initialize SyosManagerController
    }

    public void start() {
        displayWelcome();

        boolean running = true;
        while (running) {
            try {
                displayMainMenu();
                String choice = ui.getUserInput();

                switch (choice) {
                    case "1" -> startCashierInterface();
                    case "2" -> startInventoryManagerInterface();
                    case "3" -> startManagerInterface();
                    case "4" -> startOnlineCustomerInterface();
                    case "5" -> {
                        ui.displaySuccess("Thank you for using SYOS!");
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
        ui.clearScreen();
        System.out.println("================================================");
        System.out.println("           SYOS - Management System");
        System.out.println("================================================");
        System.out.println("Welcome to Synex Outlet Store (SYOS)");
        System.out.println("Complete Retail Management Solution");
        System.out.println("================================================");
    }

    private void displayMainMenu() {
        ui.clearScreen();
        System.out.println("================================================");
        System.out.println("           SYOS - Management System");
        System.out.println("================================================");
        System.out.println();
        System.out.println("=== SELECT YOUR ROLE ===");
        System.out.println("1. 💳 Cashier (Physical Store POS)");
        System.out.println("2. 📦 Inventory Manager");
        System.out.println("3. 📊 SYOS Manager");
        System.out.println("4. 🛒 Online Customer");
        System.out.println("5. ⌨ Exit");
        System.out.println();
        System.out.print("Select your role: ");
    }

    private void startCashierInterface() {
        ui.clearScreen();
        System.out.println("=== CASHIER INTERFACE ===");
        cashierController.startCashierMode();
    }

    private void startInventoryManagerInterface() {
        ui.clearScreen();
        System.out.println("=== INVENTORY MANAGER INTERFACE ===");
        inventoryManagerController.startInventoryManagerMode();
    }

    private void startManagerInterface() {
        ui.clearScreen();
        System.out.println("=== SYOS MANAGER INTERFACE ===");
        syosManagerController.startManagerMode(); // Now calls the actual manager controller!
    }

    private void startOnlineCustomerInterface() {
        ui.clearScreen();
        System.out.println("=== ONLINE CUSTOMER INTERFACE ===");
        onlineCustomerController.start();
    }
}