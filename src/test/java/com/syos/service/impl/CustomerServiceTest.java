package com.syos.service.impl;

import com.syos.domain.models.Customer;
import com.syos.exceptions.CustomerNotFoundException;
import com.syos.exceptions.CustomerRegistrationException;
import com.syos.exceptions.InvalidLoginException;
import com.syos.repository.interfaces.CustomerRepository;
import com.syos.service.interfaces.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerServiceImpl(customerRepository);
    }

    @Test
    @DisplayName("Should register customer successfully")
    void shouldRegisterCustomerSuccessfully() {
        // Arrange
        String name = "John Doe";
        String email = "john@example.com";
        String phone = "123456789";
        String address = "123 Main St";
        String password = "password123";

        Customer expectedCustomer = new Customer(1, name, email, phone, address,
                Customer.hashPassword(password),
                LocalDate.now(), true);

        when(customerRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(customerRepository.saveCustomer(eq(name), eq(email), eq(phone),
                eq(address), anyString()))
                .thenReturn(expectedCustomer);

        // Act
        Customer result = customerService.registerCustomer(name, email, phone, address, password);

        // Assert
        assertNotNull(result);
        assertEquals(expectedCustomer.getCustomerId(), result.getCustomerId());
        assertEquals(expectedCustomer.getCustomerName(), result.getCustomerName());
        assertEquals(expectedCustomer.getEmail(), result.getEmail());

        verify(customerRepository).findByEmail(email);
        verify(customerRepository).saveCustomer(eq(name), eq(email), eq(phone),
                eq(address), anyString());
    }

    @Test
    @DisplayName("Should throw exception when registering with existing email")
    void shouldThrowExceptionWhenRegisteringWithExistingEmail() {
        // Arrange
        String email = "existing@example.com";
        Customer existingCustomer = new Customer(1, "Existing User", email, "123", "Address",
                "hash", LocalDate.now(), true);

        when(customerRepository.findByEmail(email)).thenReturn(Optional.of(existingCustomer));

        // Act & Assert
        CustomerRegistrationException exception = assertThrows(CustomerRegistrationException.class,
                () -> customerService.registerCustomer("New User", email, "456", "New Address", "password"));

        assertTrue(exception.getMessage().contains("Email already registered"));
        verify(customerRepository).findByEmail(email);
        verify(customerRepository, never()).saveCustomer(anyString(), anyString(),
                anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw exception for invalid email format")
    void shouldThrowExceptionForInvalidEmailFormat() {
        // Arrange
        String invalidEmail = "notanemail";

        // Act & Assert
        CustomerRegistrationException exception = assertThrows(CustomerRegistrationException.class,
                () -> customerService.registerCustomer("John Doe", invalidEmail, "123", "Address", "password"));

        assertTrue(exception.getMessage().contains("Invalid email format"));
        verify(customerRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("Should throw exception for short password")
    void shouldThrowExceptionForShortPassword() {
        // Arrange
        String shortPassword = "123"; // Less than 6 characters

        // Act & Assert
        CustomerRegistrationException exception = assertThrows(CustomerRegistrationException.class,
                () -> customerService.registerCustomer("John Doe", "john@example.com", "123", "Address", shortPassword));

        assertTrue(exception.getMessage().contains("Password must be at least 6 characters"));
        verify(customerRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("Should throw exception for empty name")
    void shouldThrowExceptionForEmptyName() {
        // Act & Assert
        CustomerRegistrationException exception = assertThrows(CustomerRegistrationException.class,
                () -> customerService.registerCustomer("", "john@example.com", "123", "Address", "password123"));

        assertTrue(exception.getMessage().contains("Customer name is required"));
    }

    @Test
    @DisplayName("Should login customer successfully")
    void shouldLoginCustomerSuccessfully() {
        // Arrange
        String email = "john@example.com";
        String password = "password123";
        String passwordHash = Customer.hashPassword(password);

        Customer customer = new Customer(1, "John Doe", email, "123", "Address",
                passwordHash, LocalDate.now(), true);

        when(customerRepository.findByEmailWithPassword(email))
                .thenReturn(Optional.of(customer));

        // Act
        Customer result = customerService.loginCustomer(email, password);

        // Assert
        assertNotNull(result);
        assertEquals(customer.getCustomerId(), result.getCustomerId());
        assertEquals(customer.getEmail(), result.getEmail());

        verify(customerRepository).findByEmailWithPassword(email);
    }

    @Test
    @DisplayName("Should throw exception for invalid login credentials")
    void shouldThrowExceptionForInvalidLoginCredentials() {
        // Arrange
        String email = "john@example.com";
        String wrongPassword = "wrongpassword";

        when(customerRepository.findByEmailWithPassword(email))
                .thenReturn(Optional.empty());

        // Act & Assert
        InvalidLoginException exception = assertThrows(InvalidLoginException.class,
                () -> customerService.loginCustomer(email, wrongPassword));

        assertTrue(exception.getMessage().contains("Invalid email or password"));
        verify(customerRepository).findByEmailWithPassword(email);
    }

    @Test
    @DisplayName("Should throw exception for wrong password")
    void shouldThrowExceptionForWrongPassword() {
        // Arrange
        String email = "john@example.com";
        String correctPassword = "correctpassword";
        String wrongPassword = "wrongpassword";
        String passwordHash = Customer.hashPassword(correctPassword);

        Customer customer = new Customer(1, "John Doe", email, "123", "Address",
                passwordHash, LocalDate.now(), true);

        when(customerRepository.findByEmailWithPassword(email))
                .thenReturn(Optional.of(customer));

        // Act & Assert
        InvalidLoginException exception = assertThrows(InvalidLoginException.class,
                () -> customerService.loginCustomer(email, wrongPassword));

        assertTrue(exception.getMessage().contains("Invalid email or password"));
    }

    @Test
    @DisplayName("Should find customer by ID successfully")
    void shouldFindCustomerByIdSuccessfully() {
        // Arrange
        Integer customerId = 1;
        Customer customer = new Customer(customerId, "John Doe", "john@example.com",
                "123", "Address", "hash", LocalDate.now(), true);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        // Act
        Customer result = customerService.findCustomerById(customerId);

        // Assert
        assertNotNull(result);
        assertEquals(customer.getCustomerId(), result.getCustomerId());

        verify(customerRepository).findById(customerId);
    }

    @Test
    @DisplayName("Should throw exception when customer not found by ID")
    void shouldThrowExceptionWhenCustomerNotFoundById() {
        // Arrange
        Integer customerId = 999;
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        // Act & Assert
        CustomerNotFoundException exception = assertThrows(CustomerNotFoundException.class,
                () -> customerService.findCustomerById(customerId));

        assertTrue(exception.getMessage().contains("Customer not found with ID: 999"));
        verify(customerRepository).findById(customerId);
    }

    @Test
    @DisplayName("Should return true when email is registered")
    void shouldReturnTrueWhenEmailIsRegistered() {
        // Arrange
        String email = "existing@example.com";
        Customer customer = new Customer(1, "Existing User", email, "123", "Address",
                "hash", LocalDate.now(), true);

        when(customerRepository.findByEmail(email)).thenReturn(Optional.of(customer));

        // Act
        boolean result = customerService.isEmailRegistered(email);

        // Assert
        assertTrue(result);
        verify(customerRepository).findByEmail(email);
    }

    @Test
    @DisplayName("Should return false when email is not registered")
    void shouldReturnFalseWhenEmailIsNotRegistered() {
        // Arrange
        String email = "newuser@example.com";
        when(customerRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act
        boolean result = customerService.isEmailRegistered(email);

        // Assert
        assertFalse(result);
        verify(customerRepository).findByEmail(email);
    }
}