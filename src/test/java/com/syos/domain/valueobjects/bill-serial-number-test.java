// BillSerialNumberTest.java
package com.syos.domain.valueobjects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

class BillSerialNumberTest {
    
    @Test
    @DisplayName("Should create bill serial number with valid serial")
    void shouldCreateBillSerialNumberWithValidSerial() {
        String serialNumber = "BILL000001";
        BillSerialNumber billSerial = new BillSerialNumber(serialNumber);
        
        assertEquals(serialNumber, billSerial.getSerialNumber());
    }
    
    @Test
    @DisplayName("Should trim whitespace from serial number")
    void shouldTrimWhitespaceFromSerialNumber() {
        String serialWithWhitespace = "  BILL000001  ";
        BillSerialNumber billSerial = new BillSerialNumber(serialWithWhitespace);
        
        assertEquals("BILL000001", billSerial.getSerialNumber());
    }
    
    @Test
    @DisplayName("Should throw exception for null serial number")
    void shouldThrowExceptionForNullSerialNumber() {
        assertThrows(IllegalArgumentException.class, () -> new BillSerialNumber(null));
    }
    
    @Test
    @DisplayName("Should throw exception for empty serial number")
    void shouldThrowExceptionForEmptySerialNumber() {
        assertThrows(IllegalArgumentException.class, () -> new BillSerialNumber(""));
        assertThrows(IllegalArgumentException.class, () -> new BillSerialNumber("   "));
    }
    
    @Test
    @DisplayName("Should be equal when serial numbers are same")
    void shouldBeEqualWhenSerialNumbersAreSame() {
        BillSerialNumber serial1 = new BillSerialNumber("BILL000001");
        BillSerialNumber serial2 = new BillSerialNumber("BILL000001");
        
        assertEquals(serial1, serial2);
        assertEquals(serial1.hashCode(), serial2.hashCode());
    }
    
    @Test
    @DisplayName("Should not be equal when serial numbers are different")
    void shouldNotBeEqualWhenSerialNumbersAreDifferent() {
        BillSerialNumber serial1 = new BillSerialNumber("BILL000001");
        BillSerialNumber serial2 = new BillSerialNumber("BILL000002");
        
        assertNotEquals(serial1, serial2);
    }
    
    @Test
    @DisplayName("Should format toString correctly")
    void shouldFormatToStringCorrectly() {
        String serialNumber = "BILL000001";
        BillSerialNumber billSerial = new BillSerialNumber(serialNumber);
        
        assertEquals(serialNumber, billSerial.toString());
    }
}