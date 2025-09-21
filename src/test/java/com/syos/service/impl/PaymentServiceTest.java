// PaymentServiceTest.java
package com.syos.service.impl;

import com.syos.domain.models.Bill;
import com.syos.domain.models.BillItem;
import com.syos.domain.valueobjects.*;
import com.syos.domain.enums.*;
import com.syos.exceptions.InvalidPaymentException;
import com.syos.service.interfaces.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;

class PaymentServiceTest {

    private PaymentService paymentService;
    private Bill testBill;

    @BeforeEach
    void setUp() {
        paymentService = new CashPaymentServiceImpl();

        testBill = new Bill(
            new BillSerialNumber("BILL000001"), null,
            TransactionType.CASH, StoreType.PHYSICAL,
            new Money(0.0), LocalDate.now()
        );

        // Add a test item to create a total
        testBill.addItem(new BillItem(
            new ProductCode("BVEDRB001"),
            "Red Bull Energy Drink",
            2,
            new Money(250.0),
            1
        ));
    }

    @Test
    @DisplayName("Should process cash payment successfully")
    void shouldProcessCashPaymentSuccessfully() {
        Money cashTendered = new Money(600.0);

        assertDoesNotThrow(() -> paymentService.processCashPayment(testBill, cashTendered));

        assertEquals(cashTendered, testBill.getCashTendered());
        assertEquals(new Money(100.0), testBill.getChangeAmount());
    }

    @Test
    @DisplayName("Should throw exception for insufficient payment")
    void shouldThrowExceptionForInsufficientPayment() {
        Money insufficientCash = new Money(400.0);

        assertThrows(InvalidPaymentException.class,
            () -> paymentService.processCashPayment(testBill, insufficientCash));
    }

    @Test
    @DisplayName("Should calculate change correctly")
    void shouldCalculateChangeCorrectly() {
        Money totalAmount = new Money(500.0);
        Money cashTendered = new Money(600.0);

        Money change = paymentService.calculateChange(totalAmount, cashTendered);

        assertEquals(new Money(100.0), change);
    }

    @Test
    @DisplayName("Should throw exception when cash tendered is less than total")
    void shouldThrowExceptionWhenCashTenderedIsLessThanTotal() {
        Money totalAmount = new Money(500.0);
        Money insufficientCash = new Money(400.0);

        assertThrows(InvalidPaymentException.class,
            () -> paymentService.calculateChange(totalAmount, insufficientCash));
    }

    @Test
    @DisplayName("Should check if payment is sufficient")
    void shouldCheckIfPaymentIsSufficient() {
        Money totalAmount = new Money(500.0);
        Money sufficientCash = new Money(500.0);
        Money moreThanSufficient = new Money(600.0);
        Money insufficientCash = new Money(400.0);

        assertTrue(paymentService.isPaymentSufficient(totalAmount, sufficientCash));
        assertTrue(paymentService.isPaymentSufficient(totalAmount, moreThanSufficient));
        assertFalse(paymentService.isPaymentSufficient(totalAmount, insufficientCash));
    }
}