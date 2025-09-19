package com.syos.domain.models;

import java.time.LocalDate;

public class Customer {
    private final Integer customerId;
    private final String customerName;
    private final String email;
    private final String phone;
    private final String address;
    private final String passwordHash;
    private final LocalDate registrationDate;
    private final boolean isActive;

    public Customer(Integer customerId, String customerName, String email,
                    String phone, String address, String passwordHash,
                    LocalDate registrationDate, boolean isActive) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.passwordHash = passwordHash;
        this.registrationDate = registrationDate;
        this.isActive = isActive;
    }

    // Getters
    public Integer getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getPasswordHash() { return passwordHash; }
    public LocalDate getRegistrationDate() { return registrationDate; }
    public boolean isActive() { return isActive; }

    // Password verification method
    public boolean verifyPassword(String plainPassword) {
        // Simple hash comparison for now
        // In production, use proper password hashing (BCrypt, etc.)
        return this.passwordHash.equals(hashPassword(plainPassword));
    }

    // Simple password hashing (for demo - use BCrypt in production)
    public static String hashPassword(String plainPassword) {
        // Simple hash for demo - replace with BCrypt in production
        return "HASH_" + plainPassword.hashCode();
    }

    @Override
    public String toString() {
        return String.format("Customer{id=%d, name='%s', email='%s'}",
                customerId, customerName, email);
    }
}