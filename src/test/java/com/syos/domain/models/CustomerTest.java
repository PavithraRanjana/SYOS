package com.syos.domain.models;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;

class CustomerTest {

    @Test
    @DisplayName("Should create customer with valid parameters")
    void shouldCreateCustomerWithValidParameters() {
        // Arrange
        Integer customerId = 1;
        String customerName = "John Doe";
        String email = "john@example.com";
        String phone = "+94771234567";
        String address = "123 Main St";
        String passwordHash = "HASH_123456";
        LocalDate registrationDate = LocalDate.now();
        boolean isActive = true;

        // Act
        Customer customer = new Customer(customerId, customerName, email,
                phone, address, passwordHash,
                registrationDate, isActive);

        // Assert
        assertEquals(customerId, customer.getCustomerId());
        assertEquals(customerName, customer.getCustomerName());
        assertEquals(email, customer.getEmail());
        assertEquals(phone, customer.getPhone());
        assertEquals(address, customer.getAddress());
        assertEquals(passwordHash, customer.getPasswordHash());
        assertEquals(registrationDate, customer.getRegistrationDate());
        assertEquals(isActive, customer.isActive());
    }

    @Test
    @DisplayName("Should verify correct password")
    void shouldVerifyCorrectPassword() {
        // Arrange
        String plainPassword = "password123";
        String passwordHash = Customer.hashPassword(plainPassword);
        Customer customer = new Customer(1, "John Doe", "john@example.com",
                "123456789", "Address", passwordHash,
                LocalDate.now(), true);

        // Act & Assert
        assertTrue(customer.verifyPassword(plainPassword));
    }

    @Test
    @DisplayName("Should reject incorrect password")
    void shouldRejectIncorrectPassword() {
        // Arrange
        String correctPassword = "password123";
        String wrongPassword = "wrongpassword";
        String passwordHash = Customer.hashPassword(correctPassword);
        Customer customer = new Customer(1, "John Doe", "john@example.com",
                "123456789", "Address", passwordHash,
                LocalDate.now(), true);

        // Act & Assert
        assertFalse(customer.verifyPassword(wrongPassword));
    }

    @Test
    @DisplayName("Should hash password consistently")
    void shouldHashPasswordConsistently() {
        // Arrange
        String password = "testpassword";

        // Act
        String hash1 = Customer.hashPassword(password);
        String hash2 = Customer.hashPassword(password);

        // Assert
        assertEquals(hash1, hash2);
        assertNotEquals(password, hash1); // Hash should be different from plain password
    }

    @Test
    @DisplayName("Should format toString correctly")
    void shouldFormatToStringCorrectly() {
        // Arrange
        Customer customer = new Customer(123, "Jane Smith", "jane@example.com",
                "987654321", "456 Oak Ave", "hash",
                LocalDate.now(), true);

        // Act
        String result = customer.toString();

        // Assert
        String expected = "Customer{id=123, name='Jane Smith', email='jane@example.com'}";
        assertEquals(expected, result);
    }
}