package com.syos.domain.models;

import java.time.LocalDate;

public class Customer {
    private final Integer customerId;
    private final String customerName;
    private final String email;
    private final String phone;
    private final String address;
    private final LocalDate registrationDate;
    private final boolean isActive;

    public Customer(Integer customerId, String customerName, String email, 
                   String phone, String address, LocalDate registrationDate, boolean isActive) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.registrationDate = registrationDate;
        this.isActive = isActive;
    }

    // Getters
    public Integer getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public LocalDate getRegistrationDate() { return registrationDate; }
    public boolean isActive() { return isActive; }

    @Override
    public String toString() {
        return String.format("Customer{id=%d, name='%s', email='%s'}", 
                           customerId, customerName, email);
    }
}