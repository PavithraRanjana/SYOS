package com.syos.service.interfaces;

import com.syos.domain.valueobjects.Money;
import com.syos.domain.models.Bill;

public interface PaymentService {
    void processCashPayment(Bill bill, Money cashTendered);
    Money calculateChange(Money totalAmount, Money cashTendered);
    boolean isPaymentSufficient(Money totalAmount, Money cashTendered);
}
