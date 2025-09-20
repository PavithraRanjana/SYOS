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

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnlineBillingServiceTest {

    @Mock
    private ProductService productService;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private BillRepository billRepository;

    private BillingService billingService;
    private Customer testCustomer;
    private Product testProduct;
    private ProductCode testProductCode;
    private MainInventory testBatch;

    @BeforeEach
    void setUp() {
        billingService = new BillingServiceImpl(
                productService, inventoryService, paymentService, billRepository);

        testCustomer = new Customer(1, "John Doe", "john@example.com",
                "123456789", "123 Main St", "hashedpassword",
                LocalDate.now(), true);

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
    @DisplayName("Should create new online bill successfully")
    void shouldCreateNewOnlineBillSuccessfully() {
        // Arrange
        LocalDate billDate = LocalDate.now();
        BillSerialNumber expectedSerial = new BillSerialNumber("BILL000001");

        when(billRepository.generateNextSerialNumber(billDate))
                .thenReturn(expectedSerial);

        // Act
        Bill result = billingService.createNewOnlineBill(testCustomer, billDate);

        // Assert
        assertNotNull(result);
        assertEquals(expectedSerial, result.getBillSerialNumber());
        assertEquals(testCustomer.getCustomerId(), result.getCustomerId());
        assertEquals(TransactionType.ONLINE, result.getTransactionType());
        assertEquals(StoreType.ONLINE, result.getStoreType());
        assertTrue(result.isEmpty());
        verify(billRepository).generateNextSerialNumber(billDate);
    }

    @Test
    @DisplayName("Should throw exception when creating online bill without customer")
    void shouldThrowExceptionWhenCreatingOnlineBillWithoutCustomer() {
        // Act & Assert
        BillingException exception = assertThrows(BillingException.class,
                () -> billingService.createNewOnlineBill(null, LocalDate.now()));

        assertTrue(exception.getMessage().contains("Customer is required for online orders"));
    }

    @Test
    @DisplayName("Should add item to online bill successfully")
    void shouldAddItemToOnlineBillSuccessfully() {
        // Arrange
        Bill bill = new Bill(
                new BillSerialNumber("BILL000001"), testCustomer.getCustomerId(),
                TransactionType.ONLINE, StoreType.ONLINE,
                new Money(0.0), LocalDate.now()
        );

        int quantity = 2;

        when(productService.findProductByCode(testProductCode))
                .thenReturn(testProduct);
        when(inventoryService.isProductAvailableOnline(testProductCode, quantity))
                .thenReturn(true);
        when(inventoryService.reserveOnlineStock(testProductCode, quantity))
                .thenReturn(testBatch);

        // Act
        BillItem result = billingService.addItemToOnlineBill(bill, testProductCode, quantity);

        // Assert
        assertNotNull(result);
        assertEquals(testProductCode, result.getProductCode());
        assertEquals(testProduct.getProductName(), result.getProductName());
        assertEquals(quantity, result.getQuantity());
        assertEquals(testProduct.getUnitPrice(), result.getUnitPrice());
        assertEquals(testBatch.getBatchNumber(), result.getBatchNumber());

        assertFalse(bill.isEmpty());
        assertEquals(1, bill.getItemCount());

        verify(productService).findProductByCode(testProductCode);
        verify(inventoryService).isProductAvailableOnline(testProductCode, quantity);
        verify(inventoryService).reserveOnlineStock(testProductCode, quantity);
    }

    @Test
    @DisplayName("Should throw exception when adding item with insufficient online stock")
    void shouldThrowExceptionWhenAddingItemWithInsufficientOnlineStock() {
        // Arrange
        Bill bill = new Bill(
                new BillSerialNumber("BILL000001"), testCustomer.getCustomerId(),
                TransactionType.ONLINE, StoreType.ONLINE,
                new Money(0.0), LocalDate.now()
        );

        int quantity = 10;

        when(productService.findProductByCode(testProductCode))
                .thenReturn(testProduct);
        when(inventoryService.isProductAvailableOnline(testProductCode, quantity))
                .thenReturn(false);
        when(inventoryService.getTotalOnlineStock(testProductCode))
                .thenReturn(5);

        // Act & Assert
        InsufficientStockException exception = assertThrows(InsufficientStockException.class,
                () -> billingService.addItemToOnlineBill(bill, testProductCode, quantity));

        assertEquals(5, exception.getAvailableStock());
        assertEquals(10, exception.getRequestedQuantity());

        verify(productService).findProductByCode(testProductCode);
        verify(inventoryService).isProductAvailableOnline(testProductCode, quantity);
        verify(inventoryService).getTotalOnlineStock(testProductCode);
    }

    @Test
    @DisplayName("Should save online bill successfully")
    void shouldSaveOnlineBillSuccessfully() {
        // Arrange
        Bill bill = new Bill(
                new BillSerialNumber("BILL000001"), testCustomer.getCustomerId(),
                TransactionType.ONLINE, StoreType.ONLINE,
                new Money(0.0), LocalDate.now()
        );

        BillItem item = new BillItem(
                testProductCode, "Red Bull Energy Drink", 2,
                new Money(250.0), 1
        );
        bill.addItem(item);

        when(billRepository.saveBillWithItems(bill))
                .thenReturn(bill);

        // Act
        Bill result = billingService.saveOnlineBill(bill);

        // Assert
        assertEquals(bill, result);
        verify(billRepository).saveBillWithItems(bill);
    }

    @Test
    @DisplayName("Should get customer orders successfully")
    void shouldGetCustomerOrdersSuccessfully() {
        // Arrange
        Integer customerId = testCustomer.getCustomerId();
        List<Bill> expectedOrders = Arrays.asList(
                new Bill(new BillSerialNumber("BILL000001"), customerId,
                        TransactionType.ONLINE, StoreType.ONLINE,
                        new Money(0.0), LocalDate.now()),
                new Bill(new BillSerialNumber("BILL000002"), customerId,
                        TransactionType.ONLINE, StoreType.ONLINE,
                        new Money(0.0), LocalDate.now())
        );

        when(billRepository.findByCustomerId(customerId))
                .thenReturn(expectedOrders);

        // Act
        List<Bill> result = billingService.getCustomerOrders(customerId);

        // Assert
        assertEquals(expectedOrders, result);
        assertEquals(2, result.size());
        verify(billRepository).findByCustomerId(customerId);
    }

    @Test
    @DisplayName("Should throw exception when getting orders fails")
    void shouldThrowExceptionWhenGettingOrdersFails() {
        // Arrange
        Integer customerId = testCustomer.getCustomerId();

        when(billRepository.findByCustomerId(customerId))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        BillingException exception = assertThrows(BillingException.class,
                () -> billingService.getCustomerOrders(customerId));

        assertTrue(exception.getMessage().contains("Failed to retrieve customer orders"));
        verify(billRepository).findByCustomerId(customerId);
    }

    @Test
    @DisplayName("Should calculate running total correctly for online bill")
    void shouldCalculateRunningTotalCorrectlyForOnlineBill() {
        // Arrange
        Bill bill = new Bill(
                new BillSerialNumber("BILL000001"), testCustomer.getCustomerId(),
                TransactionType.ONLINE, StoreType.ONLINE,
                new Money(10.0), LocalDate.now() // 10 LKR discount
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

        // Act
        Money result = billingService.calculateRunningTotal(bill);

        // Assert
        // Subtotal: (250*2) + (120*3) = 500 + 360 = 860
        // Total: 860 - 10 (discount) = 850
        assertEquals(new Money(850.0), result);
    }

    @Test
    @DisplayName("Should throw exception when saving online bill fails")
    void shouldThrowExceptionWhenSavingOnlineBillFails() {
        // Arrange
        Bill bill = new Bill(
                new BillSerialNumber("BILL000001"), testCustomer.getCustomerId(),
                TransactionType.ONLINE, StoreType.ONLINE,
                new Money(0.0), LocalDate.now()
        );

        when(billRepository.saveBillWithItems(bill))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        BillingException exception = assertThrows(BillingException.class,
                () -> billingService.saveOnlineBill(bill));

        assertTrue(exception.getMessage().contains("Failed to save online order"));
        verify(billRepository).saveBillWithItems(bill);
    }
}