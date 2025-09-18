// BillItemTest.java
package com.syos.domain.models;

import com.syos.domain.valueobjects.ProductCode;
import com.syos.domain.valueobjects.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

class BillItemTest {
    
    @Test
    @DisplayName("Should create bill item with valid parameters")
    void shouldCreateBillItemWithValidParameters() {
        ProductCode productCode = new ProductCode("BVEDRB001");
        String productName = "Red Bull Energy Drink";
        int quantity = 2;
        Money unitPrice = new Money(250.0);
        int batchNumber = 1;
        
        BillItem billItem = new BillItem(productCode, productName, quantity, unitPrice, batchNumber);
        
        assertEquals(productCode, billItem.getProductCode());
        assertEquals(productName, billItem.getProductName());
        assertEquals(quantity, billItem.getQuantity());
        assertEquals(unitPrice, billItem.getUnitPrice());
        assertEquals(batchNumber, billItem.getBatchNumber());
        assertEquals(new Money(500.0), billItem.getTotalPrice()); // 250 * 2
    }
    
    @Test
    @DisplayName("Should throw exception for zero quantity")
    void shouldThrowExceptionForZeroQuantity() {
        ProductCode productCode = new ProductCode("BVEDRB001");
        String productName = "Red Bull Energy Drink";
        Money unitPrice = new Money(250.0);
        int batchNumber = 1;
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BillItem(productCode, productName, 0, unitPrice, batchNumber));
    }
    
    @Test
    @DisplayName("Should throw exception for negative quantity")
    void shouldThrowExceptionForNegativeQuantity() {
        ProductCode productCode = new ProductCode("BVEDRB001");
        String productName = "Red Bull Energy Drink";
        Money unitPrice = new Money(250.0);
        int batchNumber = 1;
        
        assertThrows(IllegalArgumentException.class, () -> 
            new BillItem(productCode, productName, -1, unitPrice, batchNumber));
    }
    
    @Test
    @DisplayName("Should calculate total price correctly for different quantities")
    void shouldCalculateTotalPriceCorrectlyForDifferentQuantities() {
        ProductCode productCode = new ProductCode("BVEDRB001");
        String productName = "Red Bull Energy Drink";
        Money unitPrice = new Money(125.50);
        int batchNumber = 1;
        
        BillItem singleItem = new BillItem(productCode, productName, 1, unitPrice, batchNumber);
        BillItem multipleItems = new BillItem(productCode, productName, 3, unitPrice, batchNumber);
        
        assertEquals(new Money(125.50), singleItem.getTotalPrice());
        assertEquals(new Money(376.50), multipleItems.getTotalPrice()); // 125.50 * 3
    }
    
    @Test
    @DisplayName("Should format toString correctly")
    void shouldFormatToStringCorrectly() {
        ProductCode productCode = new ProductCode("BVEDRB001");
        String productName = "Red Bull Energy Drink";
        int quantity = 2;
        Money unitPrice = new Money(250.0);
        int batchNumber = 1;
        
        BillItem billItem = new BillItem(productCode, productName, quantity, unitPrice, batchNumber);
        String expected = "Red Bull Energy Drink x2 = LKR 500.00";
        
        assertEquals(expected, billItem.toString());
    }
}