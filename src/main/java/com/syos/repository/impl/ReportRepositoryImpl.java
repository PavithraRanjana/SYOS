package com.syos.repository.impl;

import com.syos.repository.interfaces.ReportRepository;
import com.syos.service.interfaces.ReportService.*;
import com.syos.utils.DatabaseConnection;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of ReportRepository using MySQL database.
 * Each method is focused on a specific report query following Single Responsibility Principle.
 */
public class ReportRepositoryImpl implements ReportRepository {

    private final Connection connection;

    public ReportRepositoryImpl() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    // ==================== DAILY SALES REPORT QUERIES ====================

    @Override
    public List<SalesItem> getPhysicalStoreSalesData(LocalDate reportDate) {
        String sql = """
            SELECT p.product_code, p.product_name, 
                   SUM(bi.quantity) as total_quantity, 
                   SUM(bi.total_price) as revenue
            FROM bill b
            JOIN bill_item bi ON b.bill_id = bi.bill_id
            JOIN product p ON bi.product_code = p.product_code
            WHERE b.store_type = 'PHYSICAL' 
            AND b.bill_date = ?
            GROUP BY p.product_code, p.product_name
            ORDER BY p.product_code
        """;

        return executeSalesQuery(sql, reportDate);
    }

    @Override
    public List<SalesItem> getOnlineStoreSalesData(LocalDate reportDate) {
        String sql = """
            SELECT p.product_code, p.product_name, 
                   SUM(bi.quantity) as total_quantity, 
                   SUM(bi.total_price) as revenue
            FROM bill b
            JOIN bill_item bi ON b.bill_id = bi.bill_id
            JOIN product p ON bi.product_code = p.product_code
            WHERE b.store_type = 'ONLINE' 
            AND b.bill_date = ?
            GROUP BY p.product_code, p.product_name
            ORDER BY p.product_code
        """;

        return executeSalesQuery(sql, reportDate);
    }

    @Override
    public java.math.BigDecimal getPhysicalStoreRevenue(LocalDate reportDate) {
        String sql = """
            SELECT COALESCE(SUM(total_amount), 0) as revenue
            FROM bill 
            WHERE store_type = 'PHYSICAL' AND bill_date = ?
        """;

        return executeRevenueQuery(sql, reportDate);
    }

    @Override
    public java.math.BigDecimal getOnlineStoreRevenue(LocalDate reportDate) {
        String sql = """
            SELECT COALESCE(SUM(total_amount), 0) as revenue
            FROM bill 
            WHERE store_type = 'ONLINE' AND bill_date = ?
        """;

        return executeRevenueQuery(sql, reportDate);
    }

    // ==================== RESTOCK REPORT QUERIES ====================

    @Override
    public List<RestockItem> getPhysicalStoreRestockNeeds(LocalDate reportDate) {
        String sql = """
            SELECT p.product_code, p.product_name,
                   COALESCE(SUM(psi.quantity_on_shelf), 0) as current_quantity,
                   GREATEST(0, 70 - COALESCE(SUM(psi.quantity_on_shelf), 0)) as quantity_needed
            FROM product p
            LEFT JOIN physical_store_inventory psi ON p.product_code = psi.product_code
            WHERE p.is_active = TRUE
            GROUP BY p.product_code, p.product_name
            HAVING current_quantity < 70
            ORDER BY quantity_needed DESC, p.product_code
        """;

        return executeRestockQuery(sql);
    }

    @Override
    public List<RestockItem> getOnlineStoreRestockNeeds(LocalDate reportDate) {
        String sql = """
            SELECT p.product_code, p.product_name,
                   COALESCE(SUM(osi.quantity_available), 0) as current_quantity,
                   GREATEST(0, 70 - COALESCE(SUM(osi.quantity_available), 0)) as quantity_needed
            FROM product p
            LEFT JOIN online_store_inventory osi ON p.product_code = osi.product_code
            WHERE p.is_active = TRUE
            GROUP BY p.product_code, p.product_name
            HAVING current_quantity < 70
            ORDER BY quantity_needed DESC, p.product_code
        """;

        return executeRestockQuery(sql);
    }

    // ==================== REORDER REPORT QUERIES ====================

    @Override
    public List<ReorderItem> getMainInventoryReorderNeeds(LocalDate reportDate) {
        String sql = """
            SELECT p.product_code, p.product_name,
                   COALESCE(SUM(mi.remaining_quantity), 0) as total_quantity
            FROM product p
            LEFT JOIN main_inventory mi ON p.product_code = mi.product_code
            WHERE p.is_active = TRUE 
            AND (mi.purchase_date IS NULL OR mi.purchase_date <= ?)
            GROUP BY p.product_code, p.product_name
            HAVING total_quantity < 50
            ORDER BY total_quantity ASC, p.product_code
        """;

        List<ReorderItem> items = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(reportDate));
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String status = rs.getInt("total_quantity") == 0 ? "CRITICAL" : "LOW";

                items.add(new ReorderItem(
                        rs.getString("product_code"),
                        rs.getString("product_name"),
                        rs.getInt("total_quantity"),
                        status
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching reorder report data", e);
        }

        return items;
    }

    // ==================== STOCK REPORT QUERIES ====================

    @Override
    public List<StockItem> getStockReportData(LocalDate reportDate) {
        String sql = """
            SELECT p.product_code, p.product_name,
                   mi.main_inventory_id as batch_number,
                   mi.quantity_received,
                   mi.purchase_date,
                   mi.expiry_date,
                   mi.remaining_quantity,
                   COALESCE(psi.quantity_on_shelf, 0) as physical_quantity,
                   COALESCE(osi.quantity_available, 0) as online_quantity
            FROM product p
            LEFT JOIN main_inventory mi ON p.product_code = mi.product_code
            LEFT JOIN (
                SELECT product_code, main_inventory_id, SUM(quantity_on_shelf) as quantity_on_shelf
                FROM physical_store_inventory 
                GROUP BY product_code, main_inventory_id
            ) psi ON mi.main_inventory_id = psi.main_inventory_id
            LEFT JOIN (
                SELECT product_code, main_inventory_id, SUM(quantity_available) as quantity_available
                FROM online_store_inventory 
                GROUP BY product_code, main_inventory_id
            ) osi ON mi.main_inventory_id = osi.main_inventory_id
            WHERE p.is_active = TRUE 
            AND (mi.purchase_date IS NULL OR mi.purchase_date <= ?)
            ORDER BY p.product_code, mi.purchase_date, mi.main_inventory_id
        """;

        List<StockItem> items = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(reportDate));
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                // Handle products with no batches
                if (rs.getObject("batch_number") == null) {
                    items.add(new StockItem(
                            rs.getString("product_code"),
                            rs.getString("product_name"),
                            0, // No batch number
                            0, // No quantity received
                            null, // No purchase date
                            null, // No expiry date
                            0, // No main inventory
                            0, // No physical store quantity
                            0  // No online store quantity
                    ));
                } else {
                    items.add(new StockItem(
                            rs.getString("product_code"),
                            rs.getString("product_name"),
                            rs.getInt("batch_number"),
                            rs.getInt("quantity_received"),
                            rs.getDate("purchase_date") != null ? rs.getDate("purchase_date").toLocalDate() : null,
                            rs.getDate("expiry_date") != null ? rs.getDate("expiry_date").toLocalDate() : null,
                            rs.getInt("remaining_quantity"),
                            rs.getInt("physical_quantity"),
                            rs.getInt("online_quantity")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching stock report data", e);
        }

        return items;
    }

    // ==================== BILL REPORT QUERIES ====================

    @Override
    public List<BillSummary> getPhysicalStoreBills(LocalDate reportDate) {
        String sql = """
            SELECT b.bill_serial_number, b.bill_date, 
                   NULL as customer_name,
                   COUNT(bi.bill_item_id) as item_count,
                   b.total_amount, b.transaction_type
            FROM bill b
            LEFT JOIN bill_item bi ON b.bill_id = bi.bill_id
            WHERE b.store_type = 'PHYSICAL' AND b.bill_date = ?
            GROUP BY b.bill_id, b.bill_serial_number, b.bill_date, b.total_amount, b.transaction_type
            ORDER BY b.bill_serial_number
        """;

        return executeBillQuery(sql, reportDate);
    }

    @Override
    public List<BillSummary> getOnlineStoreBills(LocalDate reportDate) {
        String sql = """
            SELECT b.bill_serial_number, b.bill_date, 
                   c.customer_name,
                   COUNT(bi.bill_item_id) as item_count,
                   b.total_amount, b.transaction_type
            FROM bill b
            LEFT JOIN customer c ON b.customer_id = c.customer_id
            LEFT JOIN bill_item bi ON b.bill_id = bi.bill_id
            WHERE b.store_type = 'ONLINE' AND b.bill_date = ?
            GROUP BY b.bill_id, b.bill_serial_number, b.bill_date, c.customer_name, b.total_amount, b.transaction_type
            ORDER BY b.bill_serial_number
        """;

        return executeBillQuery(sql, reportDate);
    }

    @Override
    public int getPhysicalStoreTransactionCount(LocalDate reportDate) {
        String sql = """
            SELECT COUNT(*) as transaction_count
            FROM bill
            WHERE store_type = 'PHYSICAL' AND bill_date = ?
        """;

        return executeCountQuery(sql, reportDate);
    }

    @Override
    public int getOnlineStoreTransactionCount(LocalDate reportDate) {
        String sql = """
            SELECT COUNT(*) as transaction_count
            FROM bill
            WHERE store_type = 'ONLINE' AND bill_date = ?
        """;

        return executeCountQuery(sql, reportDate);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private List<SalesItem> executeSalesQuery(String sql, LocalDate reportDate) {
        List<SalesItem> items = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(reportDate));
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                items.add(new SalesItem(
                        rs.getString("product_code"),
                        rs.getString("product_name"),
                        rs.getInt("total_quantity"),
                        rs.getBigDecimal("revenue")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching sales data", e);
        }

        return items;
    }

    private java.math.BigDecimal executeRevenueQuery(String sql, LocalDate reportDate) {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(reportDate));
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getBigDecimal("revenue");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching revenue data", e);
        }

        return java.math.BigDecimal.ZERO;
    }

    private List<RestockItem> executeRestockQuery(String sql) {
        List<RestockItem> items = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                items.add(new RestockItem(
                        rs.getString("product_code"),
                        rs.getString("product_name"),
                        rs.getInt("current_quantity"),
                        rs.getInt("quantity_needed")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching restock data", e);
        }

        return items;
    }

    private List<BillSummary> executeBillQuery(String sql, LocalDate reportDate) {
        List<BillSummary> bills = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(reportDate));
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                bills.add(new BillSummary(
                        rs.getString("bill_serial_number"),
                        rs.getDate("bill_date").toLocalDate(),
                        rs.getString("customer_name"),
                        rs.getInt("item_count"),
                        rs.getBigDecimal("total_amount"),
                        rs.getString("transaction_type")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching bill data", e);
        }

        return bills;
    }

    private int executeCountQuery(String sql, LocalDate reportDate) {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(reportDate));
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("transaction_count");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching transaction count", e);
        }

        return 0;
    }
}