package com.syos.controller;

import com.syos.domain.models.Bill;
import com.syos.domain.models.BillItem;
import com.syos.domain.models.Product;
import com.syos.domain.valueobjects.BillSerialNumber;
import com.syos.domain.valueobjects.ProductCode;
import com.syos.domain.valueobjects.Money;
import com.syos.domain.enums.StoreType;
import com.syos.domain.enums.TransactionType;
import com.syos.domain.enums.UnitOfMeasure;
import com.syos.exceptions.ProductNotFoundException;
import com.syos.exceptions.InsufficientStockException;
import com.syos.exceptions.InvalidPaymentException;
import com.syos.exceptions.BillingException;
import com.syos.service.interfaces.BillingService;
import com.syos.service.interfaces.ProductService;
import com.syos.ui.interfaces.UserInterface;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhysicalStoreControllerTest {

    @Mock
    private BillingService billingService;

    @Mock
    private ProductService productService;

    @Mock
    private UserInterface ui;

    private PhysicalStoreController controller;
    private Product testProduct;
    private Bill testBill;

    @BeforeEach
    void setUp() {
        controller = new PhysicalStoreController(billingService, productService, ui);

        testProduct = new Product(
                new ProductCode("TEST001"),
                "Test Product",
                1, // categoryId
                1, // subcategoryId
                1, // brandId
                new Money(new BigDecimal("10.00")),
                "Test Description",
                UnitOfMeasure.PCS, // unitOfMeasure
                true // isActive
        );

        testBill = new Bill(
                new BillSerialNumber("BILL000001"),
                null, // No customer for physical store
                TransactionType.CASH,
                StoreType.PHYSICAL,
                new Money(new BigDecimal("0.00")),
                LocalDate.now()
        );
    }

    @Test
    @DisplayName("Should start cashier mode and handle menu navigation")
    void shouldStartCashierModeAndHandleMenuNavigation() {
        // Given
        when(ui.getUserInput()).thenReturn("4"); // Exit option

        // When
        controller.startCashierMode();

        // Then
        verify(ui).clearScreen();
        verify(ui).displayMenu();
        verify(ui).displaySuccess("Returning to main menu...");
    }

    @Test
    @DisplayName("Should handle invalid menu option")
    void shouldHandleInvalidMenuOption() {
        // Given
        when(ui.getUserInput()).thenReturn("99", "4"); // Invalid option, then exit

        // When
        controller.startCashierMode();

        // Then
        verify(ui).displayError("Invalid option. Please try again.");
    }



    @Test
    @DisplayName("Should search products through menu")
    void shouldSearchProductsThroughMenu() {
        // Given
        List<Product> products = Arrays.asList(testProduct);
        when(ui.getUserInput()).thenReturn("2", "4"); // Search products, then exit
        when(ui.getUserInput("Enter search term (product code, name, category, brand) or press Enter for all: "))
                .thenReturn("test");
        when(productService.searchProducts("test")).thenReturn(products);

        // When
        controller.startCashierMode();

        // Then
        verify(productService).searchProducts("test");
        verify(ui).displayProductSearch(products);
    }

    @Test
    @DisplayName("Should view reports through menu")
    void shouldViewReportsThroughMenu() {
        // Given
        when(ui.getUserInput()).thenReturn("3", "4"); // View reports, then exit

        // When
        controller.startCashierMode();

        // Then
        verify(ui).displayError("Reports feature not yet implemented.");
    }

    @Test
    @DisplayName("Should cancel transaction when no items added through menu")
    void shouldCancelTransactionWhenNoItemsAddedThroughMenu() {
        // Given
        when(ui.getUserInput()).thenReturn("1", "4"); // Start transaction, then exit
        when(billingService.createNewBill(StoreType.PHYSICAL, LocalDate.now())).thenReturn(testBill);
        when(ui.getProductCode()).thenReturn(null); // No products added immediately

        // When
        controller.startCashierMode();

        // Then
        verify(ui).displayError("No items added to bill. Transaction cancelled.");
        verify(billingService, never()).completeBill(any(), any());
    }



    @Test
    @DisplayName("Should handle product not found during transaction through menu")
    void shouldHandleProductNotFoundDuringTransactionThroughMenu() {
        // Given
        ProductCode invalidCode = new ProductCode("INVALID001");
        when(ui.getUserInput()).thenReturn("1", "4"); // Start transaction, then exit
        when(billingService.createNewBill(StoreType.PHYSICAL, LocalDate.now())).thenReturn(testBill);
        when(ui.getProductCode()).thenReturn(invalidCode, null); // Try invalid product, then done
        when(ui.getQuantity()).thenReturn(1);
        when(billingService.addItemToBill(testBill, invalidCode, 1))
                .thenThrow(new ProductNotFoundException("Product not found: " + invalidCode));

        // When
        controller.startCashierMode();

        // Then
        verify(ui).displayError("Product not found: Product not found: " + invalidCode);
        verify(billingService, never()).completeBill(any(), any());
    }

    @Test
    @DisplayName("Should handle insufficient stock during transaction through menu")
    void shouldHandleInsufficientStockDuringTransactionThroughMenu() {
        // Given
        when(ui.getUserInput()).thenReturn("1", "4"); // Start transaction, then exit
        when(billingService.createNewBill(StoreType.PHYSICAL, LocalDate.now())).thenReturn(testBill);
        when(ui.getProductCode()).thenReturn(testProduct.getProductCode(), null); // Try to add product, then done
        when(ui.getQuantity()).thenReturn(1000); // More than available
        when(billingService.addItemToBill(testBill, testProduct.getProductCode(), 1000))
                .thenThrow(new InsufficientStockException("Insufficient stock", 100, 1000));

        // When
        controller.startCashierMode();

        // Then
        verify(ui).displayInsufficientStock("TEST001", 100, 1000);
        verify(billingService, never()).completeBill(any(), any());
    }



    @Test
    @DisplayName("Should search all products when empty search term through menu")
    void shouldSearchAllProductsWhenEmptySearchTermThroughMenu() {
        // Given
        List<Product> products = Arrays.asList(testProduct);
        when(ui.getUserInput()).thenReturn("2", "4"); // Search products, then exit
        when(ui.getUserInput("Enter search term (product code, name, category, brand) or press Enter for all: "))
                .thenReturn("");
        when(productService.searchProducts("")).thenReturn(products);

        // When
        controller.startCashierMode();

        // Then
        verify(productService).searchProducts("");
    }

    @Test
    @DisplayName("Should handle empty search results through menu")
    void shouldHandleEmptySearchResultsThroughMenu() {
        // Given
        when(ui.getUserInput()).thenReturn("2", "4"); // Search products, then exit
        when(ui.getUserInput("Enter search term (product code, name, category, brand) or press Enter for all: "))
                .thenReturn("nonexistent");
        when(productService.searchProducts("nonexistent")).thenReturn(Collections.emptyList());

        // When
        controller.startCashierMode();

        // Then
        verify(ui).displayProductSearch(Collections.emptyList());
    }

    @Test
    @DisplayName("Should handle exception during product search through menu")
    void shouldHandleExceptionDuringProductSearchThroughMenu() {
        // Given
        when(ui.getUserInput()).thenReturn("2", "4"); // Search products, then exit
        when(ui.getUserInput("Enter search term (product code, name, category, brand) or press Enter for all: "))
                .thenReturn("test");
        when(productService.searchProducts("test"))
                .thenThrow(new RuntimeException("Search service unavailable"));

        // When
        controller.startCashierMode();

        // Then
        verify(ui).displayError("Search failed: Search service unavailable");
    }

    // Helper method
    private BillItem createTestBillItem() {
        return new BillItem(
                testProduct.getProductCode(),
                testProduct.getProductName(),
                2,
                testProduct.getUnitPrice(),
                1000
        );
    }
}