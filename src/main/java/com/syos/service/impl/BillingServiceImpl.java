package com.syos.service.impl;

import com.syos.domain.models.Bill;
import com.syos.domain.models.BillItem;
import com.syos.domain.models.Product;
import com.syos.domain.models.MainInventory;
import com.syos.domain.valueobjects.ProductCode;
import com.syos.domain.valueobjects.BillSerialNumber;
import com.syos.domain.valueobjects.Money;
import com.syos.domain.enums.TransactionType;
import com.syos.domain.enums.StoreType;
import com.syos.exceptions.BillingException;
import com.syos.exceptions.InsufficientStockException;
import com.syos.repository.interfaces.BillRepository;
import com.syos.service.interfaces.BillingService;
import com.syos.service.interfaces.ProductService;
import com.syos.service.interfaces.InventoryService;
import com.syos.service.interfaces.PaymentService;

import java.time.LocalDate;

public class BillingServiceImpl implements BillingService {
    private final ProductService productService;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final BillRepository billRepository;

    public BillingServiceImpl(ProductService productService,
                              InventoryService inventoryService,
                              PaymentService paymentService,
                              BillRepository billRepository) {
        this.productService = productService;
        this.inventoryService = inventoryService;
        this.paymentService = paymentService;
        this.billRepository = billRepository;
    }

    @Override
    public Bill createNewBill(StoreType storeType, LocalDate billDate) {
        BillSerialNumber serialNumber = billRepository.generateNextSerialNumber(billDate);
        TransactionType transactionType = storeType == StoreType.PHYSICAL ?
                TransactionType.CASH : TransactionType.ONLINE;

        return new Bill(
                serialNumber,
                null, // No customer for physical store walk-ins
                transactionType,
                storeType,
                new Money(0.0), // No discount for now
                billDate
        );
    }

    @Override
    public BillItem addItemToBill(Bill bill, ProductCode productCode, int quantity) {
        if (quantity <= 0) {
            throw new BillingException("Quantity must be positive");
        }

        // Find product
        Product product = productService.findProductByCode(productCode);

        // Check stock availability
        if (!productService.isProductAvailable(productCode, quantity)) {
            int availableStock = productService.getAvailableStock(productCode);
            throw new InsufficientStockException(
                    "Insufficient stock for " + product.getProductName(),
                    availableStock, quantity);
        }

        // Reserve stock (get batch number for FIFO)
        MainInventory selectedBatch = inventoryService.reserveStock(productCode, quantity);

        // Create bill item
        BillItem billItem = new BillItem(
                productCode,
                product.getProductName(),
                quantity,
                product.getUnitPrice(),
                selectedBatch.getBatchNumber()
        );

        // Add to bill
        bill.addItem(billItem);

        return billItem;
    }

    @Override
    public void completeBill(Bill bill, Money cashTendered) {
        if (bill.isEmpty()) {
            throw new BillingException("Cannot complete empty bill");
        }

        // Process payment
        paymentService.processCashPayment(bill, cashTendered);

        // Reduce inventory for each item (this will also be done by DB triggers)
        for (BillItem item : bill.getItems()) {
            inventoryService.reducePhysicalStoreStock(
                    item.getProductCode(),
                    item.getBatchNumber(),
                    item.getQuantity()
            );
        }
    }

    @Override
    public Bill saveBill(Bill bill) {
        try {
            return billRepository.saveBillWithItems(bill);
        } catch (Exception e) {
            throw new BillingException("Failed to save bill", e);
        }
    }

    @Override
    public Money calculateRunningTotal(Bill bill) {
        return bill.getTotalAmount();
    }
}
