// BillTest.java
package com.syos.domain.models;

import com.syos.domain.valueobjects.BillSerialNumber;
import com.syos.domain.valueobjects.Money;
import com.syos.domain.valueobjects.ProductCode;
import com.syos.domain.enums.TransactionType;
import com.syos.domain.enums.StoreType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;

class BillTest {

    private Bill bill;

    @BeforeEach
    void setUp() {
        bill = new Bill(
            new BillSerialNumber("BILL000001"),
            null,
            TransactionType.CASH,
            StoreType.PHYSICAL,
            new Money(0.0),
            LocalDate.now()
        );
    }

    @Test
    @DisplayName("Should create empty bill")
    void shouldCreateEmptyBill() {
        assertTrue(bill.isEmpty());
        assertEquals(0, bill.getItemCount());
        assertEquals(new Money(0.0), bill.getSubtotal());
    }

    @Test
    @DisplayName("Should add item to bill")
    void shouldAddItemToBill() {
        BillItem item = new BillItem(
            new ProductCode("BVEDRB001"),
            "Red Bull Energy Drink",
            2,
            new Money(250.0),
            1
        );

        bill.addItem(item);

        assertFalse(bill.isEmpty());
        assertEquals(1, bill.getItemCount());
        assertEquals(new Money(500.0), bill.getSubtotal());
    }

    @Test
    @DisplayName("Should calculate total with discount")
    void shouldCalculateTotalWithDiscount() {
        Bill billWithDiscount = new Bill(
            new BillSerialNumber("BILL000002"),
            null,
            TransactionType.CASH,
            StoreType.PHYSICAL,
            new Money(50.0), // discount
            LocalDate.now()
        );

        BillItem item = new BillItem(
            new ProductCode("BVEDRB001"),
            "Red Bull Energy Drink",
            2,
            new Money(250.0),
            1
        );

        billWithDiscount.addItem(item);

        assertEquals(new Money(500.0), billWithDiscount.getSubtotal());
        assertEquals(new Money(450.0), billWithDiscount.getTotalAmount());
    }

    @Test
    @DisplayName("Should process cash payment correctly")
    void shouldProcessCashPaymentCorrectly() {
        BillItem item = new BillItem(
            new ProductCode("BVEDRB001"),
            "Red Bull Energy Drink",
            1,
            new Money(250.0),
            1
        );
        bill.addItem(item);

        Money cashTendered = new Money(300.0);
        bill.processCashPayment(cashTendered);

        assertEquals(cashTendered, bill.getCashTendered());
        assertEquals(new Money(50.0), bill.getChangeAmount());
    }

    @Test
    @DisplayName("Should throw exception for insufficient cash payment")
    void shouldThrowExceptionForInsufficientCashPayment() {
        BillItem item = new BillItem(
            new ProductCode("BVEDRB001"),
            "Red Bull Energy Drink",
            1,
            new Money(250.0),
            1
        );
        bill.addItem(item);

        Money insufficientCash = new Money(200.0);
        assertThrows(IllegalArgumentException.class, () -> bill.processCashPayment(insufficientCash));
    }
}