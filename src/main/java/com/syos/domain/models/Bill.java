package com.syos.domain.models;

import com.syos.domain.valueobjects.BillSerialNumber;
import com.syos.domain.valueobjects.Money;
import com.syos.domain.enums.TransactionType;
import com.syos.domain.enums.StoreType;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Bill {
    private final BillSerialNumber billSerialNumber;
    private final Integer customerId; // null for walk-in customers
    private final TransactionType transactionType;
    private final StoreType storeType;
    private final List<BillItem> items;
    private final Money discountAmount;
    private final LocalDate billDate;
    private Money cashTendered;
    private Money changeAmount;

    public Bill(BillSerialNumber billSerialNumber, Integer customerId,
                TransactionType transactionType, StoreType storeType,
                Money discountAmount, LocalDate billDate) {
        this.billSerialNumber = billSerialNumber;
        this.customerId = customerId;
        this.transactionType = transactionType;
        this.storeType = storeType;
        this.items = new ArrayList<>();
        this.discountAmount = discountAmount != null ? discountAmount : new Money(0.0);
        this.billDate = billDate;
    }

    public void addItem(BillItem item) {
        items.add(item);
    }

    public Money getSubtotal() {
        return items.stream()
                .map(BillItem::getTotalPrice)
                .reduce(new Money(0.0), Money::add);
    }

    public Money getTotalAmount() {
        return getSubtotal().subtract(discountAmount);
    }

    public void processCashPayment(Money cashTendered) {
        if (!cashTendered.isGreaterThanOrEqual(getTotalAmount())) {
            throw new IllegalArgumentException("Insufficient cash tendered");
        }
        this.cashTendered = cashTendered;
        this.changeAmount = cashTendered.subtract(getTotalAmount());
    }

    // Getters
    public BillSerialNumber getBillSerialNumber() { return billSerialNumber; }
    public Integer getCustomerId() { return customerId; }
    public TransactionType getTransactionType() { return transactionType; }
    public StoreType getStoreType() { return storeType; }
    public List<BillItem> getItems() { return Collections.unmodifiableList(items); }
    public Money getDiscountAmount() { return discountAmount; }
    public LocalDate getBillDate() { return billDate; }
    public Money getCashTendered() { return cashTendered; }
    public Money getChangeAmount() { return changeAmount; }
    public boolean isEmpty() { return items.isEmpty(); }
    public int getItemCount() { return items.size(); }
}
