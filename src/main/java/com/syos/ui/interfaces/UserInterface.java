package com.syos.ui.interfaces;

import com.syos.domain.models.Bill;
import com.syos.domain.models.BillItem;
import com.syos.domain.models.Product;
import com.syos.domain.valueobjects.Money;
import com.syos.domain.valueobjects.ProductCode;
import java.util.List;

public interface UserInterface {
    void displayWelcome();
    void displayMenu();
    String getUserInput();
    String getUserInput(String prompt);
    ProductCode getProductCode();
    int getQuantity();
    Money getCashAmount();
    void displayProduct(Product product);
    void displayBillItem(BillItem item);
    void displayRunningTotal(Money total);
    void displayBillSummary(Bill bill);
    void displayReceipt(Bill bill);
    void displayError(String message);
    void displaySuccess(String message);
    void displayInsufficientStock(String productName, int available, int requested);
    void displayProductSearch(List<Product> products);
    boolean confirmAction(String message);
    void clearScreen();
    void waitForEnter();
}
