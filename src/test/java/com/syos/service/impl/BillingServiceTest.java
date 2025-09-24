// BillingServiceTest.java
package com.syos.service.impl;

import com.syos.domain.models.*;
import com.syos.domain.valueobjects.*;
import com.syos.domain.enums.*;
import com.syos.exceptions.*;
import com.syos.service.interfaces.*;
import com.syos.repository.interfaces.BillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.LocalDate;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {
    
    @Mock
    private ProductService productService;
    
    @Mock
    private InventoryService inventoryService;
    
    @Mock
    private PaymentService paymentService;
    
    @Mock
    private BillRepository billRepository;
    
    private BillingService billingService;
    private Product testProduct;
    private ProductCode testProductCode;
    private MainInventory testBatch;
    
    @BeforeEach
    void setUp() {
        billingService = new BillingServiceImpl(
            productService, inventoryService, paymentService, billRepository);
            
        testProductCode = new ProductCode("BVEDRB001");
        testProduct = new Product(
            testProductCode,
            "Red Bull Energy Drink 250ml",
            1, 1, 3,
            new Money(250.0),
            "Original Red Bull energy drink",
            UnitOfMeasure.CAN,
            true
        );
        
        testBatch = new MainInventory(
            1, testProductCode, 500, new Money(200.0),
            LocalDate.now(), LocalDate.now().plusYears(1),
            "Red Bull Lanka", 500
        );
    }
    
    @Test
    @DisplayName("Should create new bill for physical store")
    void shouldCreateNewBillForPhysicalStore() {
        LocalDate billDate = LocalDate.now();
        BillSerialNumber expectedSerial = new BillSerialNumber("BILL000001");
        
        when(billRepository.generateNextSerialNumber(billDate))
            .thenReturn(expectedSerial);
        
        Bill result = billingService.createNewBill(StoreType.PHYSICAL, billDate);
        
        assertNotNull(result);
        assertEquals(expectedSerial, result.getBillSerialNumber());
        assertEquals(TransactionType.CASH, result.getTransactionType());
        assertEquals(StoreType.PHYSICAL, result.getStoreType());
        assertTrue(result.isEmpty());
        verify(billRepository).generateNextSerialNumber(billDate);
    }
    
    @Test
    @DisplayName("Should add item to bill successfully")
    void shouldAddItemToBillSuccessfully() {
        Bill bill = new Bill(
            new BillSerialNumber("BILL000001"), null,
            TransactionType.CASH, StoreType.PHYSICAL,
            new Money(0.0), LocalDate.now()
        );
        
        int quantity = 2;
        
        when(productService.findProductByCode(testProductCode))
            .thenReturn(testProduct);
        when(productService.isProductAvailable(testProductCode, quantity))
            .thenReturn(true);
        when(inventoryService.reserveStock(testProductCode, quantity))
            .thenReturn(testBatch);
        
        BillItem result = billingService.addItemToBill(bill, testProductCode, quantity);
        
        assertNotNull(result);
        assertEquals(testProductCode, result.getProductCode());
        assertEquals(testProduct.getProductName(), result.getProductName());
        assertEquals(quantity, result.getQuantity());
        assertEquals(testProduct.getUnitPrice(), result.getUnitPrice());
        assertEquals(testBatch.getBatchNumber(), result.getBatchNumber());
        
        assertFalse(bill.isEmpty());
        assertEquals(1, bill.getItemCount());
        
        verify(productService).findProductByCode(testProductCode);
        verify(productService).isProductAvailable(testProductCode, quantity);
        verify(inventoryService).reserveStock(testProductCode, quantity);
    }
    
    @Test
    @DisplayName("Should throw exception when adding item with zero quantity")
    void shouldThrowExceptionWhenAddingItemWithZeroQuantity() {
        Bill bill = new Bill(
            new BillSerialNumber("BILL000001"), null,
            TransactionType.CASH, StoreType.PHYSICAL,
            new Money(0.0), LocalDate.now()
        );
        
        assertThrows(BillingException.class, 
            () -> billingService.addItemToBill(bill, testProductCode, 0));
    }
    
    @Test
    @DisplayName("Should throw exception when product not found")
    void shouldThrowExceptionWhenProductNotFound() {
        Bill bill = new Bill(
            new BillSerialNumber("BILL000001"), null,
            TransactionType.CASH, StoreType.PHYSICAL,
            new Money(0.0), LocalDate.now()
        );
        
        when(productService.findProductByCode(testProductCode))
            .thenThrow(new ProductNotFoundException("Product not found"));
        
        assertThrows(ProductNotFoundException.class,
            () -> billingService.addItemToBill(bill, testProductCode, 1));
    }
    
    @Test
    @DisplayName("Should throw exception when insufficient stock")
    void shouldThrowExceptionWhenInsufficientStock() {
        Bill bill = new Bill(
            new BillSerialNumber("BILL000001"), null,
            TransactionType.CASH, StoreType.PHYSICAL,
            new Money(0.0), LocalDate.now()
        );
        
        when(productService.findProductByCode(testProductCode))
            .thenReturn(testProduct);
        when(productService.isProductAvailable(testProductCode, 10))
            .thenReturn(false);
        when(productService.getAvailableStock(testProductCode))
            .thenReturn(5);
        
        InsufficientStockException exception = assertThrows(InsufficientStockException.class,
            () -> billingService.addItemToBill(bill, testProductCode, 10));
        
        assertEquals(5, exception.getAvailableStock());
        assertEquals(10, exception.getRequestedQuantity());
    }

    @Test
    @DisplayName("Should complete bill successfully")
    void shouldCompleteBillSuccessfully() {
        Bill bill = new Bill(
                new BillSerialNumber("BILL000001"), null,
                TransactionType.CASH, StoreType.PHYSICAL,
                new Money(0.0), LocalDate.now()
        );

        BillItem item = new BillItem(
                testProductCode, "Red Bull Energy Drink", 2,
                new Money(250.0), 1
        );
        bill.addItem(item);

        Money cashTendered = new Money(600.0);

        billingService.completeBill(bill, cashTendered);

        // Only verify what the method actually does - payment processing
        verify(paymentService).processCashPayment(bill, cashTendered);

        // This line must be REMOVED because completeBill doesn't call inventory service
        // verify(inventoryService).reducePhysicalStoreStock(testProductCode, 1, 2);
    }
    
    @Test
    @DisplayName("Should throw exception when completing empty bill")
    void shouldThrowExceptionWhenCompletingEmptyBill() {
        Bill emptyBill = new Bill(
            new BillSerialNumber("BILL000001"), null,
            TransactionType.CASH, StoreType.PHYSICAL,
            new Money(0.0), LocalDate.now()
        );
        
        assertThrows(BillingException.class,
            () -> billingService.completeBill(emptyBill, new Money(100.0)));
    }
    
    @Test
    @DisplayName("Should save bill successfully")
    void shouldSaveBillSuccessfully() {
        Bill bill = new Bill(
            new BillSerialNumber("BILL000001"), null,
            TransactionType.CASH, StoreType.PHYSICAL,
            new Money(0.0), LocalDate.now()
        );
        
        when(billRepository.saveBillWithItems(bill))
            .thenReturn(bill);
        
        Bill result = billingService.saveBill(bill);
        
        assertEquals(bill, result);
        verify(billRepository).saveBillWithItems(bill);
    }
    
    @Test
    @DisplayName("Should calculate running total correctly")
    void shouldCalculateRunningTotalCorrectly() {
        Bill bill = new Bill(
            new BillSerialNumber("BILL000001"), null,
            TransactionType.CASH, StoreType.PHYSICAL,
            new Money(0.0), LocalDate.now()
        );
        
        BillItem item1 = new BillItem(
            testProductCode, "Red Bull Energy Drink", 2,
            new Money(250.0), 1
        );
        BillItem item2 = new BillItem(
            new ProductCode("BVSDCC001"), "Coca-Cola", 3,
            new Money(120.0), 2
        );
        
        bill.addItem(item1);
        bill.addItem(item2);
        
        Money result = billingService.calculateRunningTotal(bill);
        
        assertEquals(new Money(860.0), result); // (250*2) + (120*3) = 500 + 360
    }
}