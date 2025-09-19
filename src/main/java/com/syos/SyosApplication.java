package com.syos;

import com.syos.controller.MainMenuController;
import com.syos.repository.impl.BillRepositoryImpl;
import com.syos.repository.impl.InventoryRepositoryImpl;
import com.syos.repository.impl.ProductRepositoryImpl;
import com.syos.repository.impl.CustomerRepositoryImpl;
import com.syos.service.impl.ProductServiceImpl;
import com.syos.service.impl.InventoryServiceImpl;
import com.syos.service.impl.BillingServiceImpl;
import com.syos.service.impl.CashPaymentServiceImpl;
import com.syos.service.impl.CustomerServiceImpl;
import com.syos.service.impl.OnlineStoreServiceImpl;
import com.syos.ui.impl.ConsoleUserInterface;
import com.syos.utils.DatabaseConnection;

public class SyosApplication {
    public static void main(String[] args) {
        try {
            // Test database connection first
            System.out.println("🚀 Initializing SYOS Application...");
            DatabaseConnection.getInstance().testConnection();

            // Initialize repositories
            ProductRepositoryImpl productRepository = new ProductRepositoryImpl();
            InventoryRepositoryImpl inventoryRepository = new InventoryRepositoryImpl();
            BillRepositoryImpl billRepository = new BillRepositoryImpl();
            CustomerRepositoryImpl customerRepository = new CustomerRepositoryImpl();

            // Initialize services
            ProductServiceImpl productService = new ProductServiceImpl(productRepository, inventoryRepository);
            InventoryServiceImpl inventoryService = new InventoryServiceImpl(inventoryRepository);
            CashPaymentServiceImpl paymentService = new CashPaymentServiceImpl();
            CustomerServiceImpl customerService = new CustomerServiceImpl(customerRepository);
            OnlineStoreServiceImpl onlineStoreService = new OnlineStoreServiceImpl(productRepository, inventoryRepository);

            BillingServiceImpl billingService = new BillingServiceImpl(
                    productService, inventoryService, paymentService, billRepository);

            // Initialize UI
            ConsoleUserInterface ui = new ConsoleUserInterface();

            // Initialize main controller
            MainMenuController mainController = new MainMenuController(
                    billingService, productService, onlineStoreService, customerService, ui);

            // Start application
            mainController.start();

            // Cleanup
            ui.close();

        } catch (Exception e) {
            System.err.println("Failed to start SYOS application: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}