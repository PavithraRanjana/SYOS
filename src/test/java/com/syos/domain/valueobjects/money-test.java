// MoneyTest.java
package com.syos.domain.valueobjects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;

class MoneyTest {
    
    @Test
    @DisplayName("Should create money with valid amount")
    void shouldCreateMoneyWithValidAmount() {
        Money money = new Money(100.50);
        assertEquals(new BigDecimal("100.50"), money.getAmount());
    }
    
    @Test
    @DisplayName("Should throw exception for negative amount")
    void shouldThrowExceptionForNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () -> new Money(-10.0));
    }
    
    @Test
    @DisplayName("Should throw exception for null amount")
    void shouldThrowExceptionForNullAmount() {
        assertThrows(IllegalArgumentException.class, () -> new Money((BigDecimal) null));
    }
    
    @Test
    @DisplayName("Should add money correctly")
    void shouldAddMoneyCorrectly() {
        Money money1 = new Money(100.0);
        Money money2 = new Money(50.0);
        Money result = money1.add(money2);
        
        assertEquals(new Money(150.0), result);
    }
    
    @Test
    @DisplayName("Should subtract money correctly")
    void shouldSubtractMoneyCorrectly() {
        Money money1 = new Money(100.0);
        Money money2 = new Money(30.0);
        Money result = money1.subtract(money2);
        
        assertEquals(new Money(70.0), result);
    }
    
    @Test
    @DisplayName("Should throw exception when subtraction results in negative")
    void shouldThrowExceptionWhenSubtractionResultsInNegative() {
        Money money1 = new Money(50.0);
        Money money2 = new Money(100.0);
        
        assertThrows(IllegalArgumentException.class, () -> money1.subtract(money2));
    }
    
    @Test
    @DisplayName("Should multiply by quantity correctly")
    void shouldMultiplyByQuantityCorrectly() {
        Money money = new Money(25.50);
        Money result = money.multiply(3);
        
        assertEquals(new Money(76.50), result);
    }
    
    @Test
    @DisplayName("Should compare money amounts correctly")
    void shouldCompareMoneyAmountsCorrectly() {
        Money money1 = new Money(100.0);
        Money money2 = new Money(50.0);
        Money money3 = new Money(100.0);
        
        assertTrue(money1.isGreaterThan(money2));
        assertFalse(money2.isGreaterThan(money1));
        assertTrue(money1.isGreaterThanOrEqual(money3));
    }
}