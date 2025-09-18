// ProductServiceTest.java
package com.syos.service.impl;

import com.syos.domain.models.Product;
import com.syos.domain.valueobjects.ProductCode;
import com.syos.domain.valueobjects.Money;
import com.syos.domain.enums.UnitOfMeasure;
import com.syos.exceptions.ProductNotFoundException;
import com.syos.repository.interfaces.ProductRepository;
import com.syos.repository.interfaces.InventoryRepository;
import com.syos.service.interfaces.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    
    @Mock
    private ProductRepository productRepository;
    
    @Mock
    private InventoryRepository inventoryRepository;
    
    private ProductService productService;
    private Product testProduct;
    private ProductCode testProductCode;
    
    @BeforeEach
    void setUp() {
        productService = new ProductServiceImpl(productRepository, inventoryRepository);
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
    }
    
    @Test
    @DisplayName("Should find product by code when exists")
    void shouldFindProductByCodeWhenExists() {
        when(productRepository.findByCodeAndActive(testProductCode))
            .thenReturn(Optional.of(testProduct));
        
        Product result = productService.findProductByCode(testProductCode);
        
        assertNotNull(result);
        assertEquals(testProduct.getProductCode(), result.getProductCode());
        assertEquals(testProduct.getProductName(), result.getProductName());
        verify(productRepository).findByCodeAndActive(testProductCode);
    }
    
    @Test
    @DisplayName("Should throw exception when product not found")
    void shouldThrowExceptionWhenProductNotFound() {
        when(productRepository.findByCodeAndActive(testProductCode))
            .thenReturn(Optional.empty());
        
        assertThrows(ProductNotFoundException.class, 
            () -> productService.findProductByCode(testProductCode));
        
        verify(productRepository).findByCodeAndActive(testProductCode);
    }
    
    @Test
    @DisplayName("Should search products with search term")
    void shouldSearchProductsWithSearchTerm() {
        String searchTerm = "Red Bull";
        List<Product> expectedProducts = Arrays.asList(testProduct);
        
        when(productRepository.searchByTerm(searchTerm))
            .thenReturn(expectedProducts);
        
        List<Product> result = productService.searchProducts(searchTerm);
        
        assertEquals(expectedProducts, result);
        verify(productRepository).searchByTerm(searchTerm);
    }
    
    @Test
    @DisplayName("Should return all active products when search term is empty")
    void shouldReturnAllActiveProductsWhenSearchTermIsEmpty() {
        List<Product> expectedProducts = Arrays.asList(testProduct);
        
        when(productRepository.findActiveProducts())
            .thenReturn(expectedProducts);
        
        List<Product> result = productService.searchProducts("");
        
        assertEquals(expectedProducts, result);
        verify(productRepository).findActiveProducts();
        verify(productRepository, never()).searchByTerm(any());
    }
    
    @Test
    @DisplayName("Should get available stock for product")
    void shouldGetAvailableStockForProduct() {
        int expectedStock = 100;
        when(inventoryRepository.getTotalPhysicalStock(testProductCode))
            .thenReturn(expectedStock);
        
        int result = productService.getAvailableStock(testProductCode);
        
        assertEquals(expectedStock, result);
        verify(inventoryRepository).getTotalPhysicalStock(testProductCode);
    }
    
    @Test
    @DisplayName("Should check if product is available with sufficient quantity")
    void shouldCheckIfProductIsAvailableWithSufficientQuantity() {
        when(inventoryRepository.getTotalPhysicalStock(testProductCode))
            .thenReturn(50);
        
        assertTrue(productService.isProductAvailable(testProductCode, 30));
        assertFalse(productService.isProductAvailable(testProductCode, 60));
    }
}