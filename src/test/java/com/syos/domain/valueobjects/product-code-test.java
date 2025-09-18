// ProductCodeTest.java
package com.syos.domain.valueobjects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

class ProductCodeTest {
    
    @Test
    @DisplayName("Should create product code with valid code")
    void shouldCreateProductCodeWithValidCode() {
        ProductCode productCode = new ProductCode("BVEDRB001");
        assertEquals("BVEDRB001", productCode.getCode());
    }
    
    @Test
    @DisplayName("Should convert to uppercase")
    void shouldConvertToUppercase() {
        ProductCode productCode = new ProductCode("bvedrb001");
        assertEquals("BVEDRB001", productCode.getCode());
    }
    
    @Test
    @DisplayName("Should trim whitespace")
    void shouldTrimWhitespace() {
        ProductCode productCode = new ProductCode("  BVEDRB001  ");
        assertEquals("BVEDRB001", productCode.getCode());
    }
    
    @Test
    @DisplayName("Should throw exception for null code")
    void shouldThrowExceptionForNullCode() {
        assertThrows(IllegalArgumentException.class, () -> new ProductCode(null));
    }
    
    @Test
    @DisplayName("Should throw exception for empty code")
    void shouldThrowExceptionForEmptyCode() {
        assertThrows(IllegalArgumentException.class, () -> new ProductCode(""));
        assertThrows(IllegalArgumentException.class, () -> new ProductCode("   "));
    }
    
    @Test
    @DisplayName("Should throw exception for code too long")
    void shouldThrowExceptionForCodeTooLong() {
        String longCode = "VERYLONGPRODUCTCODE123456";
        assertThrows(IllegalArgumentException.class, () -> new ProductCode(longCode));
    }
    
    @Test
    @DisplayName("Should be equal when codes are same")
    void shouldBeEqualWhenCodesAreSame() {
        ProductCode code1 = new ProductCode("BVEDRB001");
        ProductCode code2 = new ProductCode("BVEDRB001");
        
        assertEquals(code1, code2);
        assertEquals(code1.hashCode(), code2.hashCode());
    }
}