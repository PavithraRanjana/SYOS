package com.syos.repository.interfaces;

import com.syos.domain.models.Customer;
import java.util.Optional;

public interface CustomerRepository extends Repository<Customer, Integer> {
    Optional<Customer> findByEmail(String email);
    Customer saveCustomer(String name, String email, String phone, String address);
    boolean existsByEmail(String email);
}