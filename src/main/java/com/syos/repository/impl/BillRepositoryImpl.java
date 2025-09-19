package com.syos.repository.impl;

import com.syos.domain.models.Bill;
import com.syos.domain.models.BillItem;
import com.syos.domain.valueobjects.BillSerialNumber;
import com.syos.domain.valueobjects.ProductCode;
import com.syos.domain.valueobjects.Money;
import com.syos.domain.enums.TransactionType;
import com.syos.domain.enums.StoreType;
import com.syos.repository.interfaces.BillRepository;
import com.syos.utils.DatabaseConnection;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BillRepositoryImpl implements BillRepository {
    private final Connection connection;

    public BillRepositoryImpl() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    @Override
    public BillSerialNumber generateNextSerialNumber(LocalDate billDate) {
        String sql = """
            SELECT COALESCE(MAX(CAST(SUBSTRING(bill_serial_number, 5) AS UNSIGNED)), 0) + 1 as next_number
            FROM bill
            WHERE bill_date = ?
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(billDate));
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int nextNumber = rs.getInt("next_number");
                return new BillSerialNumber(String.format("BILL%06d", nextNumber));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error generating bill serial number", e);
        }

        return new BillSerialNumber("BILL000001");
    }

    @Override
    public Bill saveBillWithItems(Bill bill) {
        String billSql = """
            INSERT INTO bill (bill_serial_number, customer_id, transaction_type, store_type,
                             subtotal, discount_amount, total_amount, cash_tendered,
                             change_amount, bill_date)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        String itemSql = """
            INSERT INTO bill_item (bill_id, product_code, main_inventory_id, quantity,
                                  unit_price, total_price)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try {
            connection.setAutoCommit(false);

            // Save bill
            int billId;
            try (PreparedStatement billStmt = connection.prepareStatement(billSql, Statement.RETURN_GENERATED_KEYS)) {
                billStmt.setString(1, bill.getBillSerialNumber().getSerialNumber());
                billStmt.setObject(2, bill.getCustomerId());
                billStmt.setString(3, bill.getTransactionType().name());
                billStmt.setString(4, bill.getStoreType().name());
                billStmt.setBigDecimal(5, bill.getSubtotal().getAmount());
                billStmt.setBigDecimal(6, bill.getDiscountAmount().getAmount());
                billStmt.setBigDecimal(7, bill.getTotalAmount().getAmount());
                billStmt.setBigDecimal(8, bill.getCashTendered() != null ? bill.getCashTendered().getAmount() : null);
                billStmt.setBigDecimal(9, bill.getChangeAmount() != null ? bill.getChangeAmount().getAmount() : null);
                billStmt.setDate(10, Date.valueOf(bill.getBillDate()));

                billStmt.executeUpdate();

                ResultSet generatedKeys = billStmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    billId = generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Failed to get generated bill ID");
                }
            }

            // Save bill items
            try (PreparedStatement itemStmt = connection.prepareStatement(itemSql)) {
                for (BillItem item : bill.getItems()) {
                    itemStmt.setInt(1, billId);
                    itemStmt.setString(2, item.getProductCode().getCode());
                    itemStmt.setInt(3, item.getBatchNumber());
                    itemStmt.setInt(4, item.getQuantity());
                    itemStmt.setBigDecimal(5, item.getUnitPrice().getAmount());
                    itemStmt.setBigDecimal(6, item.getTotalPrice().getAmount());
                    itemStmt.addBatch();
                }
                itemStmt.executeBatch();
            }

            connection.commit();
            return bill;

        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                throw new RuntimeException("Error during rollback", rollbackEx);
            }
            throw new RuntimeException("Error saving bill with items", e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                throw new RuntimeException("Error resetting auto-commit", e);
            }
        }
    }

    @Override
    public Optional<Bill> findById(Integer billId) {
        // Implementation for finding bill by ID
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<Bill> findByDate(LocalDate date) {
        // Implementation for finding bills by date
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<Bill> findByDateRange(LocalDate startDate, LocalDate endDate) {
        // Implementation for finding bills by date range
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<Bill> findAll() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Bill save(Bill entity) {
        return saveBillWithItems(entity);
    }

    @Override
    public void delete(Integer billId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean existsById(Integer billId) {
        return findById(billId).isPresent();
    }

    @Override
    public List<Bill> findByCustomerId(Integer customerId) {
        String billSql = """
        SELECT bill_id, bill_serial_number, customer_id, transaction_type, store_type,
               subtotal, discount_amount, total_amount, cash_tendered, change_amount, bill_date
        FROM bill
        WHERE customer_id = ? AND store_type = 'ONLINE'
        ORDER BY bill_date DESC
    """;

        String itemSql = """
        SELECT bi.product_code, bi.quantity, bi.unit_price, bi.total_price, bi.main_inventory_id,
               p.product_name
        FROM bill_item bi
        JOIN product p ON bi.product_code = p.product_code
        WHERE bi.bill_id = ?
    """;

        List<Bill> bills = new ArrayList<>();

        try (PreparedStatement billStmt = connection.prepareStatement(billSql)) {
            billStmt.setInt(1, customerId);
            ResultSet billRs = billStmt.executeQuery();

            while (billRs.next()) {
                // Create bill object
                Bill bill = new Bill(
                        new BillSerialNumber(billRs.getString("bill_serial_number")),
                        billRs.getInt("customer_id"),
                        TransactionType.valueOf(billRs.getString("transaction_type")),
                        StoreType.valueOf(billRs.getString("store_type")),
                        new Money(billRs.getBigDecimal("discount_amount")),
                        billRs.getDate("bill_date").toLocalDate()
                );

                // Get bill items for this bill
                int billId = billRs.getInt("bill_id");
                try (PreparedStatement itemStmt = connection.prepareStatement(itemSql)) {
                    itemStmt.setInt(1, billId);
                    ResultSet itemRs = itemStmt.executeQuery();

                    while (itemRs.next()) {
                        BillItem item = new BillItem(
                                new ProductCode(itemRs.getString("product_code")),
                                itemRs.getString("product_name"),
                                itemRs.getInt("quantity"),
                                new Money(itemRs.getBigDecimal("unit_price")),
                                itemRs.getInt("main_inventory_id")
                        );
                        bill.addItem(item);
                    }
                }

                bills.add(bill);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding bills by customer ID", e);
        }

        return bills;
    }

}