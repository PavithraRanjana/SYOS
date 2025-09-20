package com.syos.service.impl;

import com.syos.domain.models.Product;
import com.syos.domain.valueobjects.ProductCode;
import com.syos.domain.valueobjects.Money;
import com.syos.domain.enums.UnitOfMeasure;
import com.syos.repository.interfaces.InventoryRepository;
import com.syos.repository.interfaces.ProductRepository;
import com.syos.service.interfaces.OnlineStoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnlineStoreServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    private OnlineStoreService onlineStoreService;

    @BeforeEach
    void setUp() {
        onlineStoreService = new OnlineStoreServiceImpl(productRepository, inventoryRepository);
    }

    @Test
    @DisplayName("Should get products by category successfully")
    void shouldGetProductsByCategorySuccessfully() {
        // Arrange
        List<Product> products = Arrays.asList(
                new Product(new ProductCode("BVEDRB001"), "Red Bull Energy Drink", 1, 1, 1,
                        new Money(250.0), "Energy drink", UnitOfMeasure.CAN, true),
                new Product(new ProductCode("BVSDCC001"), "Coca-Cola 330ml", 1, 4, 2,
                        new Money(120.0), "Soft drink", UnitOfMeasure.BOTTLE, true),
                new Product(new ProductCode("CHDKLIN001"), "Lindt Dark Chocolate", 2, 5, 3,
                        new Money(850.0), "Dark chocolate", UnitOfMeasure.BAR, true)
        );

        when(productRepository.findActiveProducts()).thenReturn(products);

        // Act
        Map<String, List<Product>> result = onlineStoreService.getProductsByCategory();

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("Beverages"));
        assertTrue(result.containsKey("Chocolate"));

        List<Product> beverages = result.get("Beverages");
        assertEquals(2, beverages.size());

        List<Product> chocolates = result.get("Chocolate");
        assertEquals(1, chocolates.size());

        verify(productRepository).findActiveProducts();
    }

    @Test
    @DisplayName("Should get products in specific category")
    void shouldGetProductsInSpecificCategory() {
        // Arrange
        List<Product> allProducts = Arrays.asList(
                new Product(new ProductCode("BVEDRB001"), "Red Bull Energy Drink", 1, 1, 1,
                        new Money(250.0), "Energy drink", UnitOfMeasure.CAN, true),
                new Product(new ProductCode("CHDKLIN001"), "Lindt Dark Chocolate", 2, 5, 3,
                        new Money(850.0), "Dark chocolate", UnitOfMeasure.BAR, true)
        );

        when(productRepository.findActiveProducts()).thenReturn(allProducts);

        // Act
        List<Product> beverages = onlineStoreService.getProductsInCategory("Beverages");

        // Assert
        assertNotNull(beverages);
        assertEquals(1, beverages.size());
        assertEquals("Red Bull Energy Drink", beverages.get(0).getProductName());

        verify(productRepository).findActiveProducts();
    }

    @Test
    @DisplayName("Should get available stock for product")
    void shouldGetAvailableStockForProduct() {
        // Arrange
        ProductCode productCode = new ProductCode("BVEDRB001");
        int expectedStock = 50;

        when(inventoryRepository.getTotalOnlineStock(productCode)).thenReturn(expectedStock);

        // Act
        int result = onlineStoreService.getAvailableStock(productCode);

        // Assert
        assertEquals(expectedStock, result);
        verify(inventoryRepository).getTotalOnlineStock(productCode);
    }

    @Test
    @DisplayName("Should return true when product is available online")
    void shouldReturnTrueWhenProductIsAvailableOnline() {
        // Arrange
        ProductCode productCode = new ProductCode("BVEDRB001");
        int availableStock = 50;
        int requiredQuantity = 10;

        when(inventoryRepository.getTotalOnlineStock(productCode)).thenReturn(availableStock);

        // Act
        boolean result = onlineStoreService.isProductAvailableOnline(productCode, requiredQuantity);

        // Assert
        assertTrue(result);
        verify(inventoryRepository).getTotalOnlineStock(productCode);
    }

    @Test
    @DisplayName("Should return false when product is not available in sufficient quantity")
    void shouldReturnFalseWhenProductNotAvailableInSufficientQuantity() {
        // Arrange
        ProductCode productCode = new ProductCode("BVEDRB001");
        int availableStock = 5;
        int requiredQuantity = 10;

        when(inventoryRepository.getTotalOnlineStock(productCode)).thenReturn(availableStock);

        // Act
        boolean result = onlineStoreService.isProductAvailableOnline(productCode, requiredQuantity);

        // Assert
        assertFalse(result);
        verify(inventoryRepository).getTotalOnlineStock(productCode);
    }

    @Test
    @DisplayName("Should return empty map when no products available")
    void shouldReturnEmptyMapWhenNoProductsAvailable() {
        // Arrange
        when(productRepository.findActiveProducts()).thenReturn(Arrays.asList());

        // Act
        Map<String, List<Product>> result = onlineStoreService.getProductsByCategory();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(productRepository).findActiveProducts();
    }

    @Test
    @DisplayName("Should return empty list for non-existent category")
    void shouldReturnEmptyListForNonExistentCategory() {
        // Arrange
        List<Product> allProducts = Arrays.asList(
                new Product(new ProductCode("BVEDRB001"), "Red Bull Energy Drink", 1, 1, 1,
                        new Money(250.0), "Energy drink", UnitOfMeasure.CAN, true)
        );

        when(productRepository.findActiveProducts()).thenReturn(allProducts);

        // Act
        List<Product> result = onlineStoreService.getProductsInCategory("NonExistentCategory");

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(productRepository).findActiveProducts();
    }
}