package com.syos.repository.impl;

import com.syos.domain.models.Customer;
import com.syos.repository.interfaces.CustomerRepository;
import com.syos.utils.DatabaseConnection;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CustomerRepositoryImpl implements CustomerRepository {
    private final Connection connection;

    public CustomerRepositoryImpl() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    @Override
    public Optional<Customer> findById(Integer customerId) {
        String sql = """
            SELECT customer_id, customer_name, email, phone, address, 
                   password_hash, registration_date, is_active
            FROM customer
            WHERE customer_id = ?
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, customerId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToCustomer(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding customer by ID", e);
        }

        return Optional.empty();
    }

    @Override
    public Optional<Customer> findByEmail(String email) {
        String sql = """
            SELECT customer_id, customer_name, email, phone, address, 
                   password_hash, registration_date, is_active
            FROM customer
            WHERE email = ? AND is_active = TRUE
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToCustomer(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding customer by email", e);
        }

        return Optional.empty();
    }

    @Override
    public Optional<Customer> findByEmailWithPassword(String email) {
        // Same as findByEmail - includes password hash
        return findByEmail(email);
    }

    @Override
    public Customer saveCustomer(String name, String email, String phone, String address, String passwordHash) {
        String sql = """
            INSERT INTO customer (customer_name, email, phone, address, password_hash, registration_date, is_active)
            VALUES (?, ?, ?, ?, ?, ?, TRUE)
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.setString(2, email);
            stmt.setString(3, phone);
            stmt.setString(4, address);
            stmt.setString(5, passwordHash);
            stmt.setDate(6, Date.valueOf(LocalDate.now()));

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int customerId = generatedKeys.getInt(1);
                    return new Customer(customerId, name, email, phone, address,
                            passwordHash, LocalDate.now(), true);
                }
            }

            throw new SQLException("Creating customer failed, no ID obtained");

        } catch (SQLException e) {
            throw new RuntimeException("Error saving customer", e);
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }

    @Override
    public List<Customer> findAll() {
        String sql = """
            SELECT customer_id, customer_name, email, phone, address, 
                   password_hash, registration_date, is_active
            FROM customer
            WHERE is_active = TRUE
            ORDER BY customer_name
        """;

        List<Customer> customers = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                customers.add(mapResultSetToCustomer(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding all customers", e);
        }

        return customers;
    }

    @Override
    public Customer save(Customer entity) {
        throw new UnsupportedOperationException("Use saveCustomer method instead");
    }

    @Override
    public void delete(Integer customerId) {
        String sql = "UPDATE customer SET is_active = FALSE WHERE customer_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, customerId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error deactivating customer", e);
        }
    }

    @Override
    public boolean existsById(Integer customerId) {
        return findById(customerId).isPresent();
    }

    private Customer mapResultSetToCustomer(ResultSet rs) throws SQLException {
        return new Customer(
                rs.getInt("customer_id"),
                rs.getString("customer_name"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("address"),
                rs.getString("password_hash"),
                rs.getDate("registration_date").toLocalDate(),
                rs.getBoolean("is_active")
        );
    }
}