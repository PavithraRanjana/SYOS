package com.syos.service.impl;

import com.syos.domain.models.Customer;
import com.syos.exceptions.CustomerNotFoundException;
import com.syos.exceptions.CustomerRegistrationException;
import com.syos.exceptions.InvalidLoginException;
import com.syos.repository.interfaces.CustomerRepository;
import com.syos.service.interfaces.CustomerService;

public class CustomerServiceImpl implements CustomerService {
    private final CustomerRepository customerRepository;

    public CustomerServiceImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public Customer registerCustomer(String name, String email, String phone, String address, String password) {
        // Validate input
        if (name == null || name.trim().isEmpty()) {
            throw new CustomerRegistrationException("Customer name is required");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new CustomerRegistrationException("Email is required");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new CustomerRegistrationException("Password is required");
        }
        if (password.trim().length() < 6) {
            throw new CustomerRegistrationException("Password must be at least 6 characters long");
        }
        if (!isValidEmail(email)) {
            throw new CustomerRegistrationException("Invalid email format");
        }

        // Check if email is already registered
        if (isEmailRegistered(email)) {
            throw new CustomerRegistrationException("Email already registered: " + email);
        }

        // Hash the password
        String passwordHash = Customer.hashPassword(password.trim());

        // Create and save customer
        try {
            return customerRepository.saveCustomer(name.trim(), email.trim(),
                    phone != null ? phone.trim() : null,
                    address != null ? address.trim() : null,
                    passwordHash);
        } catch (Exception e) {
            throw new CustomerRegistrationException("Failed to register customer", e);
        }
    }

    @Override
    public Customer loginCustomer(String email, String password) {
        if (email == null || email.trim().isEmpty()) {
            throw new InvalidLoginException("Email is required");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new InvalidLoginException("Password is required");
        }

        // Find customer by email
        Customer customer = customerRepository.findByEmailWithPassword(email.trim())
                .orElseThrow(() -> new InvalidLoginException("Invalid email or password"));

        // Verify password
        if (!customer.verifyPassword(password.trim())) {
            throw new InvalidLoginException("Invalid email or password");
        }

        return customer;
    }

    @Override
    public Customer findCustomerById(Integer customerId) {
        if (customerId == null) {
            throw new CustomerNotFoundException("Customer ID is required");
        }

        return customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + customerId));
    }

    @Override
    public boolean isEmailRegistered(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        return customerRepository.findByEmail(email.trim()).isPresent();
    }

    private boolean isValidEmail(String email) {
        return email != null && email.contains("@") && email.contains(".");
    }
}