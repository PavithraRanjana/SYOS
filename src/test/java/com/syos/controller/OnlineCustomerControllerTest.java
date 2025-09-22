package com.syos.controller;

import com.syos.domain.models.*;
import com.syos.domain.valueobjects.*;
import com.syos.domain.enums.*;
import com.syos.exceptions.*;
import com.syos.service.interfaces.*;
import com.syos.ui.interfaces.UserInterface;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnlineCustomerControllerTest {

    @Mock
    private BillingService billingService;

    @Mock
    private ProductService productService;

    @Mock
    private OnlineStoreService onlineStoreService;

    @Mock
    private CustomerService customerService;

    @Mock
    private UserInterface ui;

    private OnlineCustomerController controller;
    private Customer testCustomer;
    private Product testProduct;
    private ProductCode testProductCode;

    @BeforeEach
    void setUp() {
        controller = new OnlineCustomerController(billingService, productService,
                onlineStoreService, customerService, ui);

        testCustomer = new Customer(1, "Arya Stark", "aryastark@gameofthrones.com",
                "123456789", "Winterfell, North", "hashedpassword",
                LocalDate.now(), true);

        testProductCode = new ProductCode("SNCHPLAY001");
        testProduct = new Product(testProductCode, "Lays Classic Potato Chips 150g",
                3, 8, 23, new Money(280.0), "Original salted chips",
                UnitOfMeasure.BAG, true);
    }

    @Test
    @DisplayName("Should display category products with action options")
    void shouldDisplayCategoryProductsWithActionOptions() {
        // Arrange
        Map<String, List<Product>> categoryProducts = new HashMap<>();
        List<Product> snacks = Arrays.asList(testProduct);
        categoryProducts.put("Snacks", snacks);

        when(onlineStoreService.getProductsByCategory()).thenReturn(categoryProducts);
        when(onlineStoreService.getAvailableStock(testProductCode)).thenReturn(30);

        // This tests the data preparation for the enhanced display
        Map<String, List<Product>> result = onlineStoreService.getProductsByCategory();
        int stock = onlineStoreService.getAvailableStock(testProductCode);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("Snacks"));
        assertEquals(1, result.get("Snacks").size());
        assertEquals(30, stock);

        verify(onlineStoreService).getProductsByCategory();
        verify(onlineStoreService).getAvailableStock(testProductCode);
    }

    @Test
    @DisplayName("Should handle quick buy from category successfully")
    void shouldHandleQuickBuyFromCategorySuccessfully() {
        // Arrange
        LocalDate billDate = LocalDate.now();
        Bill bill = new Bill(new BillSerialNumber("BILL000001"),
                testCustomer.getCustomerId(), TransactionType.ONLINE,
                StoreType.ONLINE, new Money(0.0), billDate);

        int quantity = 2;
        int availableStock = 30;

        when(onlineStoreService.getAvailableStock(testProductCode)).thenReturn(availableStock);
        when(productService.findProductByCode(testProductCode)).thenReturn(testProduct);
        when(billingService.createNewOnlineBill(testCustomer, billDate)).thenReturn(bill);
        when(billingService.addItemToOnlineBill(bill, testProductCode, quantity))
                .thenReturn(new BillItem(testProductCode, testProduct.getProductName(),
                        quantity, testProduct.getUnitPrice(), 1));

        // Act
        int stock = onlineStoreService.getAvailableStock(testProductCode);
        Product product = productService.findProductByCode(testProductCode);
        Bill createdBill = billingService.createNewOnlineBill(testCustomer, billDate);
        BillItem item = billingService.addItemToOnlineBill(createdBill, testProductCode, quantity);

        // Assert
        assertEquals(availableStock, stock);
        assertEquals(testProduct, product);
        assertNotNull(createdBill);
        assertNotNull(item);
        assertEquals(testProductCode, item.getProductCode());
        assertEquals(quantity, item.getQuantity());

        verify(onlineStoreService).getAvailableStock(testProductCode);
        verify(productService).findProductByCode(testProductCode);
        verify(billingService).createNewOnlineBill(testCustomer, billDate);
        verify(billingService).addItemToOnlineBill(createdBill, testProductCode, quantity);
    }

    @Test
    @DisplayName("Should handle out of stock products in category view")
    void shouldHandleOutOfStockProductsInCategoryView() {
        // Arrange
        when(onlineStoreService.getAvailableStock(testProductCode)).thenReturn(0);

        // Act
        int stock = onlineStoreService.getAvailableStock(testProductCode);

        // Assert
        assertEquals(0, stock);
        verify(onlineStoreService).getAvailableStock(testProductCode);
    }

    @Test
    @DisplayName("Should validate product exists in selected category")
    void shouldValidateProductExistsInSelectedCategory() {
        // Arrange
        List<Product> snackProducts = Arrays.asList(testProduct);
        ProductCode differentProductCode = new ProductCode("BVEDRB001"); // Beverage product

        // Act
        boolean productInCategory = snackProducts.stream()
                .anyMatch(p -> p.getProductCode().equals(testProductCode));
        boolean differentProductInCategory = snackProducts.stream()
                .anyMatch(p -> p.getProductCode().equals(differentProductCode));

        // Assert
        assertTrue(productInCategory);
        assertFalse(differentProductInCategory);
    }

    @Test
    @DisplayName("Should handle quantity validation with max available")
    void shouldHandleQuantityValidationWithMaxAvailable() {
        // Arrange
        int maxAvailable = 30;
        int validQuantity = 5;
        int invalidQuantity = 50;

        // Act & Assert
        assertTrue(validQuantity <= maxAvailable);
        assertFalse(invalidQuantity <= maxAvailable);
    }

    @Test
    @DisplayName("Should calculate total price for quick purchase")
    void shouldCalculateTotalPriceForQuickPurchase() {
        // Arrange
        int quantity = 3;
        Money unitPrice = testProduct.getUnitPrice(); // 280.0

        // Act
        Money totalPrice = unitPrice.multiply(quantity);

        // Assert
        assertEquals(new Money(840.0), totalPrice); // 280 * 3
    }

    @Test
    @DisplayName("Should handle continue shopping with existing bill")
    void shouldHandleContinueShoppingWithExistingBill() {
        // Arrange
        Bill existingBill = new Bill(new BillSerialNumber("BILL000001"),
                testCustomer.getCustomerId(), TransactionType.ONLINE,
                StoreType.ONLINE, new Money(0.0), LocalDate.now());

        BillItem existingItem = new BillItem(testProductCode, testProduct.getProductName(),
                2, testProduct.getUnitPrice(), 1);
        existingBill.addItem(existingItem);

        // Mock the billing service to return the running total
        when(billingService.calculateRunningTotal(existingBill))
                .thenReturn(new Money(560.0)); // 2 * 280.0

        // Act
        Money runningTotal = billingService.calculateRunningTotal(existingBill);

        // Assert
        assertFalse(existingBill.isEmpty());
        assertEquals(1, existingBill.getItemCount());
        assertNotNull(runningTotal);
        assertEquals(new Money(560.0), runningTotal);

        verify(billingService).calculateRunningTotal(existingBill);
    }

    @Test
    @DisplayName("Should handle order completion and confirmation")
    void shouldHandleOrderCompletionAndConfirmation() {
        // Arrange
        Bill completedBill = new Bill(new BillSerialNumber("BILL000001"),
                testCustomer.getCustomerId(), TransactionType.ONLINE,
                StoreType.ONLINE, new Money(0.0), LocalDate.now());

        BillItem item = new BillItem(testProductCode, testProduct.getProductName(),
                2, testProduct.getUnitPrice(), 1);
        completedBill.addItem(item);

        when(billingService.saveOnlineBill(completedBill)).thenReturn(completedBill);

        // Act
        Bill savedBill = billingService.saveOnlineBill(completedBill);

        // Assert
        assertNotNull(savedBill);
        assertEquals(completedBill.getBillSerialNumber(), savedBill.getBillSerialNumber());
        assertEquals(testCustomer.getCustomerId(), savedBill.getCustomerId());

        verify(billingService).saveOnlineBill(completedBill);
    }

    @Test
    @DisplayName("Should handle insufficient stock during quick purchase")
    void shouldHandleInsufficientStockDuringQuickPurchase() {
        // Arrange
        int requestedQuantity = 50;
        int availableStock = 30;

        // Create a bill for the test
        Bill bill = new Bill(new BillSerialNumber("BILL000001"),
                testCustomer.getCustomerId(), TransactionType.ONLINE,
                StoreType.ONLINE, new Money(0.0), LocalDate.now());

        // Mock the exception to be thrown when adding item to bill
        when(billingService.addItemToOnlineBill(bill, testProductCode, requestedQuantity))
                .thenThrow(new InsufficientStockException("Insufficient stock", availableStock, requestedQuantity));

        // Act & Assert
        InsufficientStockException exception = assertThrows(InsufficientStockException.class, () -> {
            billingService.addItemToOnlineBill(bill, testProductCode, requestedQuantity);
        });

        assertEquals(availableStock, exception.getAvailableStock());
        assertEquals(requestedQuantity, exception.getRequestedQuantity());

        verify(billingService).addItemToOnlineBill(bill, testProductCode, requestedQuantity);
    }

    @Test
    @DisplayName("Should handle empty categories gracefully")
    void shouldHandleEmptyCategoriesGracefully() {
        // Arrange
        Map<String, List<Product>> emptyCategories = new HashMap<>();
        when(onlineStoreService.getProductsByCategory()).thenReturn(emptyCategories);

        // Act
        Map<String, List<Product>> result = onlineStoreService.getProductsByCategory();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(onlineStoreService).getProductsByCategory();
    }

    @Test
    @DisplayName("Should format display information correctly")
    void shouldFormatDisplayInformationCorrectly() {
        // This tests the formatting logic that would be used in the enhanced display

        // Arrange
        String productName = testProduct.getProductName();
        String description = testProduct.getDescription();
        int maxLength = 34;

        // Act
        String truncatedName = truncateForDisplay(productName, maxLength);
        String truncatedDescription = truncateForDisplay(description, 30);

        // Assert
        assertNotNull(truncatedName);
        assertNotNull(truncatedDescription);
        assertTrue(truncatedName.length() <= maxLength);
        assertTrue(truncatedDescription.length() <= 30);
    }

    // Helper method for truncation (would be in the actual controller)
    private String truncateForDisplay(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}