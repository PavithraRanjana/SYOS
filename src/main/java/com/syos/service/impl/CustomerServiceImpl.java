package com.syos.service.impl;

import com.syos.domain.models.Customer;
import com.syos.exceptions.CustomerNotFoundException;
import com.syos.exceptions.CustomerRegistrationException;
import com.syos.repository.interfaces.CustomerRepository;
import com.syos.service.interfaces.CustomerService;

public class CustomerServiceImpl implements CustomerService {
    private final CustomerRepository customerRepository;

    public CustomerServiceImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public Customer registerCustomer(String name, String email, String phone, String address) {
        // Validate input
        if (name == null || name.trim().isEmpty()) {
            throw new CustomerRegistrationException("Customer name is required");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new CustomerRegistrationException("Email is required");
        }
        if (!isValidEmail(email)) {
            throw new CustomerRegistrationException("Invalid email format");
        }
        
        // Check if email is already registered
        if (isEmailRegistered(email)) {
            throw new CustomerRegistrationException("Email already registered: " + email);
        }

        // Create and save customer
        try {
            return customerRepository.saveCustomer(name.trim(), email.trim(), 
                                                 phone != null ? phone.trim() : null, 
                                                 address != null ? address.trim() : null);
        } catch (Exception e) {
            throw new CustomerRegistrationException("Failed to register customer", e);
        }
    }

    @Override
    public Customer findCustomerByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new CustomerNotFoundException("Email is required");
        }

        return customerRepository.findByEmail(email.trim())
            .orElseThrow(() -> new CustomerNotFoundException("Customer not found with email: " + email));
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