package com.syos;

import com.syos.controller.PhysicalStoreController;
import com.syos.repository.impl.BillRepositoryImpl;
import com.syos.repository.impl.InventoryRepositoryImpl;
import com.syos.repository.impl.ProductRepositoryImpl;
import com.syos.service.impl.ProductServiceImpl;
import com.syos.service.impl.InventoryServiceImpl;
import com.syos.service.impl.BillingServiceImpl;
import com.syos.service.impl.CashPaymentServiceImpl;
import com.syos.ui.impl.ConsoleUserInterface;

public class SyosApplication {
    public static void main(String[] args) {
        try {
            // Initialize repositories
            ProductRepositoryImpl productRepository = new ProductRepositoryImpl();
            InventoryRepositoryImpl inventoryRepository = new InventoryRepositoryImpl();
            BillRepositoryImpl billRepository = new BillRepositoryImpl();

            // Initialize services
            ProductServiceImpl productService = new ProductServiceImpl(productRepository, inventoryRepository);
            InventoryServiceImpl inventoryService = new InventoryServiceImpl(inventoryRepository);
            CashPaymentServiceImpl paymentService = new CashPaymentServiceImpl();
            BillingServiceImpl billingService = new BillingServiceImpl(
                    productService, inventoryService, paymentService, billRepository);

            // Initialize UI
            ConsoleUserInterface ui = new ConsoleUserInterface();

            // Initialize controller
            PhysicalStoreController controller = new PhysicalStoreController(
                    billingService, productService, ui);

            // Start application
            controller.start();

            // Cleanup
            ui.close();

        } catch (Exception e) {
            System.err.println("Failed to start SYOS application: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
