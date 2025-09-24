package com.syos.service.impl;

import com.syos.domain.models.Bill;
import com.syos.domain.models.BillItem;
import com.syos.domain.models.Product;
import com.syos.domain.models.Customer;
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
import java.util.List;

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
    public Bill createNewOnlineBill(Customer customer, LocalDate billDate) {
        if (customer == null) {
            throw new BillingException("Customer is required for online orders");
        }

        BillSerialNumber serialNumber = billRepository.generateNextSerialNumber(billDate);

        return new Bill(
                serialNumber,
                customer.getCustomerId(),
                TransactionType.ONLINE,
                StoreType.ONLINE,
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

        // Check stock availability based on store type
        boolean isAvailable = bill.getStoreType() == StoreType.PHYSICAL ?
                productService.isProductAvailable(productCode, quantity) :
                inventoryService.isProductAvailableOnline(productCode, quantity);

        if (!isAvailable) {
            int availableStock = bill.getStoreType() == StoreType.PHYSICAL ?
                    productService.getAvailableStock(productCode) :
                    inventoryService.getTotalOnlineStock(productCode);

            throw new InsufficientStockException(
                    "Insufficient stock for " + product.getProductName(),
                    availableStock, quantity);
        }

        // Reserve stock (get batch number for FIFO)
        MainInventory selectedBatch = bill.getStoreType() == StoreType.PHYSICAL ?
                inventoryService.reserveStock(productCode, quantity) :
                inventoryService.reserveOnlineStock(productCode, quantity);

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
    public BillItem addItemToOnlineBill(Bill bill, ProductCode productCode, int quantity) {
        // Delegate to main method - the store type handling is already there
        return addItemToBill(bill, productCode, quantity);
    }

    @Override
    public void completeBill(Bill bill, Money cashTendered) {
        if (bill.isEmpty()) {
            throw new BillingException("Cannot complete empty bill");
        }

        // Process payment
        paymentService.processCashPayment(bill, cashTendered);

        // Note: Inventory reduction is handled by database triggers
        // when bill is saved, so we don't manually reduce here
    }



    @Override
    public Bill saveBill(Bill bill) {
        try {
            // For physical store bills, save and let trigger handle inventory
            return billRepository.saveBillWithItems(bill);
        } catch (Exception e) {
            throw new BillingException("Failed to save bill", e);
        }
    }

    @Override
    public Bill saveOnlineBill(Bill bill) {
        try {
            // For online orders, just save the bill
            // Database trigger will automatically reduce online inventory
            return billRepository.saveBillWithItems(bill);
        } catch (Exception e) {
            throw new BillingException("Failed to save online order", e);
        }
    }

    @Override
    public Money calculateRunningTotal(Bill bill) {
        return bill.getTotalAmount();
    }

    @Override
    public List<Bill> getCustomerOrders(Integer customerId) {
        try {
            return billRepository.findByCustomerId(customerId);
        } catch (Exception e) {
            throw new BillingException("Failed to retrieve customer orders", e);
        }
    }
}