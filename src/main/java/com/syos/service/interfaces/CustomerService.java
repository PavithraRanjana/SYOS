package com.syos.service.interfaces;

import com.syos.domain.models.Customer;

public interface CustomerService {
    Customer registerCustomer(String name, String email, String phone, String address);
    Customer findCustomerByEmail(String email);
    Customer findCustomerById(Integer customerId);
    boolean isEmailRegistered(String email);
}