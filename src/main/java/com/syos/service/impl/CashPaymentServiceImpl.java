package com.syos.service.impl;

import com.syos.domain.models.Bill;
import com.syos.domain.valueobjects.Money;
import com.syos.exceptions.InvalidPaymentException;
import com.syos.service.interfaces.PaymentService;

public class CashPaymentServiceImpl implements PaymentService {

    @Override
    public void processCashPayment(Bill bill, Money cashTendered) {
        if (!isPaymentSufficient(bill.getTotalAmount(), cashTendered)) {
            throw new InvalidPaymentException(
                    String.format("Insufficient payment. Required: %s, Tendered: %s",
                            bill.getTotalAmount(), cashTendered));
        }

        bill.processCashPayment(cashTendered);
    }

    @Override
    public Money calculateChange(Money totalAmount, Money cashTendered) {
        if (!cashTendered.isGreaterThanOrEqual(totalAmount)) {
            throw new InvalidPaymentException("Cash tendered is less than total amount");
        }
        return cashTendered.subtract(totalAmount);
    }

    @Override
    public boolean isPaymentSufficient(Money totalAmount, Money cashTendered) {
        return cashTendered.isGreaterThanOrEqual(totalAmount);
    }
}