package com.syos.service.interfaces;

import com.syos.domain.models.Bill;
import com.syos.domain.models.BillItem;
import com.syos.domain.models.Customer;
import com.syos.domain.valueobjects.ProductCode;
import com.syos.domain.valueobjects.Money;
import com.syos.domain.enums.StoreType;
import java.time.LocalDate;
import java.util.List;

public interface BillingService {
    Bill createNewBill(StoreType storeType, LocalDate billDate);
    Bill createNewOnlineBill(Customer customer, LocalDate billDate);
    BillItem addItemToBill(Bill bill, ProductCode productCode, int quantity);
    BillItem addItemToOnlineBill(Bill bill, ProductCode productCode, int quantity);
    void completeBill(Bill bill, Money cashTendered);
    Bill saveBill(Bill bill);
    Bill saveOnlineBill(Bill bill);
    Money calculateRunningTotal(Bill bill);
    List<Bill> getCustomerOrders(Integer customerId);
}