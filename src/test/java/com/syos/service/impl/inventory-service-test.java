// InventoryServiceTest.java
package com.syos.service.impl;

import com.syos.domain.models.MainInventory;
import com.syos.domain.models.PhysicalStoreInventory;
import com.syos.domain.valueobjects.ProductCode;
import com.syos.domain.valueobjects.Money;
import com.syos.exceptions.InsufficientStockException;
import com.syos.repository.interfaces.InventoryRepository;
import com.syos.service.interfaces.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.LocalDate;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {
    
    @Mock
    private InventoryRepository inventoryRepository;
    
    private InventoryService inventoryService;
    private ProductCode testProductCode;
    private MainInventory testBatch;
    private PhysicalStoreInventory testPhysicalInventory;
    
    @BeforeEach
    void setUp() {
        inventoryService = new InventoryServiceImpl(inventoryRepository);
        testProductCode = new ProductCode("BVEDRB001");
        
        testBatch = new MainInventory(
            1, testProductCode, 500, new Money(200.0),
            LocalDate.now(), LocalDate.now().plusYears(1),
            "Red Bull Lanka", 100
        );
        
        testPhysicalInventory = new PhysicalStoreInventory(
            1, testProductCode, 1, 50, LocalDate.now()
        );
    }
    
    @Test
    @DisplayName("Should reserve stock when sufficient quantity available")
    void shouldReserveStockWhenSufficientQuantityAvailable() {
        int requiredQuantity = 30;
        
        when(inventoryRepository.findNextAvailableBatch(testProductCode, requiredQuantity))
            .thenReturn(Optional.of(testBatch));
        
        MainInventory result = inventoryService.reserveStock(testProductCode, requiredQuantity);
        
        assertEquals(testBatch, result);
        verify(inventoryRepository).findNextAvailableBatch(testProductCode, requiredQuantity);
    }
    
    @Test
    @DisplayName("Should throw exception when insufficient stock for reservation")
    void shouldThrowExceptionWhenInsufficientStockForReservation() {
        int requiredQuantity = 200;
        int availableStock = 50;
        
        when(inventoryRepository.findNextAvailableBatch(testProductCode, requiredQuantity))
            .thenReturn(Optional.empty());
        when(inventoryRepository.getTotalPhysicalStock(testProductCode))
            .thenReturn(availableStock);
        
        InsufficientStockException exception = assertThrows(InsufficientStockException.class,
            () -> inventoryService.reserveStock(testProductCode, requiredQuantity));
        
        assertEquals(availableStock, exception.getAvailableStock());
        assertEquals(requiredQuantity, exception.getRequestedQuantity());
    }
    
    @Test
    @DisplayName("Should reduce physical store stock successfully")
    void shouldReducePhysicalStoreStockSuccessfully() {
        int batchNumber = 1;
        int quantityToReduce = 20;
        
        when(inventoryRepository.findPhysicalStoreStockByBatch(testProductCode, batchNumber))
            .thenReturn(Optional.of(testPhysicalInventory));
        
        inventoryService.reducePhysicalStoreStock(testProductCode, batchNumber, quantityToReduce);
        
        verify(inventoryRepository).findPhysicalStoreStockByBatch(testProductCode, batchNumber);
        verify(inventoryRepository).updatePhysicalStoreStock(testPhysicalInventory);
    }
    
    @Test
    @DisplayName("Should throw exception when physical store stock not found for batch")
    void shouldThrowExceptionWhenPhysicalStoreStockNotFoundForBatch() {
        int batchNumber = 999;
        int quantityToReduce = 20;
        
        when(inventoryRepository.findPhysicalStoreStockByBatch(testProductCode, batchNumber))
            .thenReturn(Optional.empty());
        
        assertThrows(InsufficientStockException.class,
            () -> inventoryService.reducePhysicalStoreStock(testProductCode, batchNumber, quantityToReduce));
    }
    
    @Test
    @DisplayName("Should throw exception when reducing more than available physical stock")
    void shouldThrowExceptionWhenReducingMoreThanAvailablePhysicalStock() {
        int batchNumber = 1;
        int quantityToReduce = 100; // More than the 50 available
        
        when(inventoryRepository.findPhysicalStoreStockByBatch(testProductCode, batchNumber))
            .thenReturn(Optional.of(testPhysicalInventory));
        
        // The actual implementation throws InsufficientStockException, not IllegalArgumentException
        assertThrows(InsufficientStockException.class,
            () -> inventoryService.reducePhysicalStoreStock(testProductCode, batchNumber, quantityToReduce));
    }
    
    @Test
    @DisplayName("Should get total available stock")
    void shouldGetTotalAvailableStock() {
        int expectedStock = 150;
        
        when(inventoryRepository.getTotalPhysicalStock(testProductCode))
            .thenReturn(expectedStock);
        
        int result = inventoryService.getTotalAvailableStock(testProductCode);
        
        assertEquals(expectedStock, result);
        verify(inventoryRepository).getTotalPhysicalStock(testProductCode);
    }
    
    @Test
    @DisplayName("Should find physical stock by batch")
    void shouldFindPhysicalStockByBatch() {
        int batchNumber = 1;
        
        when(inventoryRepository.findPhysicalStoreStockByBatch(testProductCode, batchNumber))
            .thenReturn(Optional.of(testPhysicalInventory));
        
        PhysicalStoreInventory result = inventoryService.findPhysicalStockByBatch(testProductCode, batchNumber);
        
        assertEquals(testPhysicalInventory, result);
        verify(inventoryRepository).findPhysicalStoreStockByBatch(testProductCode, batchNumber);
    }
    
    @Test
    @DisplayName("Should throw exception when physical stock by batch not found")
    void shouldThrowExceptionWhenPhysicalStockByBatchNotFound() {
        int batchNumber = 999;
        
        when(inventoryRepository.findPhysicalStoreStockByBatch(testProductCode, batchNumber))
            .thenReturn(Optional.empty());
        
        assertThrows(InsufficientStockException.class,
            () -> inventoryService.findPhysicalStockByBatch(testProductCode, batchNumber));
    }
}