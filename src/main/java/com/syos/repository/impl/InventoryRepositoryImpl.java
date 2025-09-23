package com.syos.repository.impl;

import com.syos.domain.models.PhysicalStoreInventory;
import com.syos.domain.models.MainInventory;
import com.syos.domain.valueobjects.ProductCode;
import com.syos.domain.valueobjects.Money;
import com.syos.repository.interfaces.InventoryRepository;
import com.syos.utils.DatabaseConnection;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Updated InventoryRepositoryImpl with all new inventory manager methods.
 */
public class InventoryRepositoryImpl implements InventoryRepository {
    private final Connection connection;

    public InventoryRepositoryImpl() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    // ==================== EXISTING METHODS (keep as they are) ====================

    @Override
    public List<PhysicalStoreInventory> findPhysicalStoreStock(ProductCode productCode) {
        String sql = """
            SELECT psi.physical_inventory_id, psi.product_code, psi.main_inventory_id,
                   psi.quantity_on_shelf, psi.restocked_date
            FROM physical_store_inventory psi
            WHERE psi.product_code = ? AND psi.quantity_on_shelf > 0
            ORDER BY psi.main_inventory_id
        """;

        List<PhysicalStoreInventory> inventories = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, productCode.getCode());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                inventories.add(new PhysicalStoreInventory(
                        rs.getInt("physical_inventory_id"),
                        new ProductCode(rs.getString("product_code")),
                        rs.getInt("main_inventory_id"),
                        rs.getInt("quantity_on_shelf"),
                        rs.getDate("restocked_date").toLocalDate()
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding physical store stock", e);
        }

        return inventories;
    }

    @Override
    public Optional<PhysicalStoreInventory> findPhysicalStoreStockByBatch(ProductCode productCode, int batchNumber) {
        String sql = """
            SELECT psi.physical_inventory_id, psi.product_code, psi.main_inventory_id,
                   psi.quantity_on_shelf, psi.restocked_date
            FROM physical_store_inventory psi
            WHERE psi.product_code = ? AND psi.main_inventory_id = ? AND psi.quantity_on_shelf > 0
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, productCode.getCode());
            stmt.setInt(2, batchNumber);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(new PhysicalStoreInventory(
                        rs.getInt("physical_inventory_id"),
                        new ProductCode(rs.getString("product_code")),
                        rs.getInt("main_inventory_id"),
                        rs.getInt("quantity_on_shelf"),
                        rs.getDate("restocked_date").toLocalDate()
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding physical store stock by batch", e);
        }

        return Optional.empty();
    }

    @Override
    public Optional<MainInventory> findNextAvailableBatch(ProductCode productCode, int requiredQuantity) {
        String sql = """
            SELECT mi.main_inventory_id, mi.product_code, mi.quantity_received,
                   mi.purchase_price, mi.purchase_date, mi.expiry_date,
                   mi.supplier_name, mi.remaining_quantity
            FROM main_inventory mi
            JOIN physical_store_inventory psi ON mi.main_inventory_id = psi.main_inventory_id
            WHERE mi.product_code = ? AND psi.quantity_on_shelf >= ?
            ORDER BY mi.expiry_date ASC, mi.purchase_date ASC
            LIMIT 1
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, productCode.getCode());
            stmt.setInt(2, requiredQuantity);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToMainInventory(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding next available batch", e);
        }

        return Optional.empty();
    }

    @Override
    public Optional<MainInventory> findNextAvailableOnlineBatch(ProductCode productCode, int requiredQuantity) {
        String sql = """
            SELECT mi.main_inventory_id, mi.product_code, mi.quantity_received,
                   mi.purchase_price, mi.purchase_date, mi.expiry_date,
                   mi.supplier_name, mi.remaining_quantity
            FROM main_inventory mi
            JOIN online_store_inventory osi ON mi.main_inventory_id = osi.main_inventory_id
            WHERE mi.product_code = ? AND osi.quantity_available >= ?
            ORDER BY mi.expiry_date ASC, mi.purchase_date ASC
            LIMIT 1
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, productCode.getCode());
            stmt.setInt(2, requiredQuantity);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToMainInventory(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding next available online batch", e);
        }

        return Optional.empty();
    }

    @Override
    public List<MainInventory> findMainInventoryBatches(ProductCode productCode) {
        String sql = """
            SELECT mi.main_inventory_id, mi.product_code, mi.quantity_received,
                   mi.purchase_price, mi.purchase_date, mi.expiry_date,
                   mi.supplier_name, mi.remaining_quantity
            FROM main_inventory mi
            WHERE mi.product_code = ? AND mi.remaining_quantity > 0
            ORDER BY mi.expiry_date ASC, mi.purchase_date ASC
        """;

        List<MainInventory> batches = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, productCode.getCode());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                batches.add(mapResultSetToMainInventory(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding main inventory batches", e);
        }

        return batches;
    }

    @Override
    public void updatePhysicalStoreStock(PhysicalStoreInventory inventory) {
        String sql = """
            UPDATE physical_store_inventory
            SET quantity_on_shelf = ?
            WHERE physical_inventory_id = ?
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, inventory.getQuantityOnShelf());
            stmt.setInt(2, inventory.getPhysicalInventoryId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating physical store stock", e);
        }
    }

    @Override
    public void updateMainInventoryStock(MainInventory inventory) {
        String sql = """
            UPDATE main_inventory
            SET remaining_quantity = ?
            WHERE main_inventory_id = ?
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, inventory.getRemainingQuantity());
            stmt.setInt(2, inventory.getBatchNumber());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating main inventory stock", e);
        }
    }

    @Override
    public void reduceOnlineStock(ProductCode productCode, int batchNumber, int quantity) {
        String sql = """
            UPDATE online_store_inventory
            SET quantity_available = quantity_available - ?
            WHERE product_code = ? AND main_inventory_id = ? AND quantity_available >= ?
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, quantity);
            stmt.setString(2, productCode.getCode());
            stmt.setInt(3, batchNumber);
            stmt.setInt(4, quantity);

            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated == 0) {
                throw new RuntimeException("Failed to reduce online stock - insufficient quantity");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error reducing online stock", e);
        }
    }

    @Override
    public int getTotalPhysicalStock(ProductCode productCode) {
        String sql = """
            SELECT COALESCE(SUM(psi.quantity_on_shelf), 0) as total_stock
            FROM physical_store_inventory psi
            WHERE psi.product_code = ?
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, productCode.getCode());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("total_stock");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting total physical stock", e);
        }

        return 0;
    }

    @Override
    public int getTotalOnlineStock(ProductCode productCode) {
        String sql = """
            SELECT COALESCE(SUM(osi.quantity_available), 0) as total_stock
            FROM online_store_inventory osi
            WHERE osi.product_code = ?
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, productCode.getCode());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("total_stock");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting total online stock", e);
        }

        return 0;
    }

    // ==================== NEW METHODS FOR INVENTORY MANAGER ====================

    @Override
    public MainInventory addNewBatch(ProductCode productCode, int quantityReceived, Money purchasePrice,
                                     LocalDate purchaseDate, LocalDate expiryDate, String supplierName) {
        String sql = """
            INSERT INTO main_inventory (product_code, quantity_received, purchase_price, 
                                      purchase_date, expiry_date, supplier_name, remaining_quantity)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, productCode.getCode());
            stmt.setInt(2, quantityReceived);
            stmt.setBigDecimal(3, purchasePrice.getAmount());
            stmt.setDate(4, Date.valueOf(purchaseDate));
            stmt.setDate(5, expiryDate != null ? Date.valueOf(expiryDate) : null);
            stmt.setString(6, supplierName);
            stmt.setInt(7, quantityReceived); // remaining_quantity starts as full quantity

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int batchNumber = generatedKeys.getInt(1);
                    return new MainInventory(batchNumber, productCode, quantityReceived,
                            purchasePrice, purchaseDate, expiryDate,
                            supplierName, quantityReceived);
                }
            }

            throw new SQLException("Creating batch failed, no ID obtained");

        } catch (SQLException e) {
            throw new RuntimeException("Error adding new batch", e);
        }
    }

    @Override
    public Optional<MainInventory> findBatchByNumber(int batchNumber) {
        String sql = """
            SELECT mi.main_inventory_id, mi.product_code, mi.quantity_received,
                   mi.purchase_price, mi.purchase_date, mi.expiry_date,
                   mi.supplier_name, mi.remaining_quantity
            FROM main_inventory mi
            WHERE mi.main_inventory_id = ?
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, batchNumber);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToMainInventory(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding batch by number", e);
        }

        return Optional.empty();
    }

    @Override
    public void removeBatch(int batchNumber) {
        String sql = "DELETE FROM main_inventory WHERE main_inventory_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, batchNumber);

            int rowsDeleted = stmt.executeUpdate();
            if (rowsDeleted == 0) {
                throw new RuntimeException("Batch not found: " + batchNumber);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error removing batch", e);
        }
    }

    @Override
    public MainInventory restoreBatch(MainInventory batch) {
        String sql = """
            INSERT INTO main_inventory (main_inventory_id, product_code, quantity_received, 
                                      purchase_price, purchase_date, expiry_date, 
                                      supplier_name, remaining_quantity)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, batch.getBatchNumber());
            stmt.setString(2, batch.getProductCode().getCode());
            stmt.setInt(3, batch.getQuantityReceived());
            stmt.setBigDecimal(4, batch.getPurchasePrice().getAmount());
            stmt.setDate(5, Date.valueOf(batch.getPurchaseDate()));
            stmt.setDate(6, batch.getExpiryDate() != null ? Date.valueOf(batch.getExpiryDate()) : null);
            stmt.setString(7, batch.getSupplierName());
            stmt.setInt(8, batch.getRemainingQuantity());

            stmt.executeUpdate();
            return batch;

        } catch (SQLException e) {
            throw new RuntimeException("Error restoring batch", e);
        }
    }

    @Override
    public void issueToPhysicalStore(ProductCode productCode, int batchNumber, int quantity) {
        String checkSql = """
            SELECT physical_inventory_id FROM physical_store_inventory 
            WHERE product_code = ? AND main_inventory_id = ?
        """;

        String updateSql = """
            UPDATE physical_store_inventory 
            SET quantity_on_shelf = quantity_on_shelf + ?, restocked_date = ?
            WHERE product_code = ? AND main_inventory_id = ?
        """;

        String insertSql = """
            INSERT INTO physical_store_inventory (product_code, main_inventory_id, quantity_on_shelf, restocked_date)
            VALUES (?, ?, ?, ?)
        """;

        try {
            connection.setAutoCommit(false);

            // Check if record exists
            boolean exists = false;
            try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
                checkStmt.setString(1, productCode.getCode());
                checkStmt.setInt(2, batchNumber);
                ResultSet rs = checkStmt.executeQuery();
                exists = rs.next();
            }

            if (exists) {
                // Update existing record
                try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                    updateStmt.setInt(1, quantity);
                    updateStmt.setDate(2, Date.valueOf(LocalDate.now()));
                    updateStmt.setString(3, productCode.getCode());
                    updateStmt.setInt(4, batchNumber);
                    updateStmt.executeUpdate();
                }
            } else {
                // Insert new record
                try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                    insertStmt.setString(1, productCode.getCode());
                    insertStmt.setInt(2, batchNumber);
                    insertStmt.setInt(3, quantity);
                    insertStmt.setDate(4, Date.valueOf(LocalDate.now()));
                    insertStmt.executeUpdate();
                }
            }

            connection.commit();

        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                throw new RuntimeException("Error during rollback", rollbackEx);
            }
            throw new RuntimeException("Error issuing to physical store", e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                throw new RuntimeException("Error resetting auto-commit", e);
            }
        }
    }

    @Override
    public void issueToOnlineStore(ProductCode productCode, int batchNumber, int quantity) {
        String checkSql = """
            SELECT online_inventory_id FROM online_store_inventory 
            WHERE product_code = ? AND main_inventory_id = ?
        """;

        String updateSql = """
            UPDATE online_store_inventory 
            SET quantity_available = quantity_available + ?, restocked_date = ?
            WHERE product_code = ? AND main_inventory_id = ?
        """;

        String insertSql = """
            INSERT INTO online_store_inventory (product_code, main_inventory_id, quantity_available, restocked_date)
            VALUES (?, ?, ?, ?)
        """;

        try {
            connection.setAutoCommit(false);

            // Check if record exists
            boolean exists = false;
            try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
                checkStmt.setString(1, productCode.getCode());
                checkStmt.setInt(2, batchNumber);
                ResultSet rs = checkStmt.executeQuery();
                exists = rs.next();
            }

            if (exists) {
                // Update existing record
                try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                    updateStmt.setInt(1, quantity);
                    updateStmt.setDate(2, Date.valueOf(LocalDate.now()));
                    updateStmt.setString(3, productCode.getCode());
                    updateStmt.setInt(4, batchNumber);
                    updateStmt.executeUpdate();
                }
            } else {
                // Insert new record
                try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                    insertStmt.setString(1, productCode.getCode());
                    insertStmt.setInt(2, batchNumber);
                    insertStmt.setInt(3, quantity);
                    insertStmt.setDate(4, Date.valueOf(LocalDate.now()));
                    insertStmt.executeUpdate();
                }
            }

            connection.commit();

        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                throw new RuntimeException("Error during rollback", rollbackEx);
            }
            throw new RuntimeException("Error issuing to online store", e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                throw new RuntimeException("Error resetting auto-commit", e);
            }
        }
    }

    @Override
    public void returnFromPhysicalStore(ProductCode productCode, int batchNumber, int quantity) {
        String sql = """
            UPDATE physical_store_inventory 
            SET quantity_on_shelf = quantity_on_shelf - ?
            WHERE product_code = ? AND main_inventory_id = ? AND quantity_on_shelf >= ?
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, quantity);
            stmt.setString(2, productCode.getCode());
            stmt.setInt(3, batchNumber);
            stmt.setInt(4, quantity);

            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated == 0) {
                throw new RuntimeException("Failed to return from physical store - insufficient quantity or record not found");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error returning from physical store", e);
        }
    }

    @Override
    public void returnFromOnlineStore(ProductCode productCode, int batchNumber, int quantity) {
        String sql = """
            UPDATE online_store_inventory 
            SET quantity_available = quantity_available - ?
            WHERE product_code = ? AND main_inventory_id = ? AND quantity_available >= ?
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, quantity);
            stmt.setString(2, productCode.getCode());
            stmt.setInt(3, batchNumber);
            stmt.setInt(4, quantity);

            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated == 0) {
                throw new RuntimeException("Failed to return from online store - insufficient quantity or record not found");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error returning from online store", e);
        }
    }

    @Override
    public void reduceMainInventoryStock(int batchNumber, int quantity) {
        String sql = """
            UPDATE main_inventory 
            SET remaining_quantity = remaining_quantity - ?
            WHERE main_inventory_id = ? AND remaining_quantity >= ?
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, quantity);
            stmt.setInt(2, batchNumber);
            stmt.setInt(3, quantity);

            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated == 0) {
                throw new RuntimeException("Failed to reduce main inventory - insufficient quantity or batch not found");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error reducing main inventory stock", e);
        }
    }

    @Override
    public void restoreMainInventoryStock(int batchNumber, int quantity) {
        String sql = """
            UPDATE main_inventory 
            SET remaining_quantity = remaining_quantity + ?
            WHERE main_inventory_id = ?
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, quantity);
            stmt.setInt(2, batchNumber);

            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated == 0) {
                throw new RuntimeException("Failed to restore main inventory - batch not found");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error restoring main inventory stock", e);
        }
    }

    @Override
    public int getTotalMainInventoryStock(ProductCode productCode) {
        String sql = """
            SELECT COALESCE(SUM(mi.remaining_quantity), 0) as total_stock
            FROM main_inventory mi
            WHERE mi.product_code = ?
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, productCode.getCode());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("total_stock");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting total main inventory stock", e);
        }

        return 0;
    }

    @Override
    public int getPhysicalStoreUsage(int batchNumber) {
        String sql = """
        SELECT COALESCE(SUM(psi.quantity_on_shelf), 0) as `usage`
        FROM physical_store_inventory psi
        WHERE psi.main_inventory_id = ?
    """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, batchNumber);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("usage");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting physical store usage", e);
        }

        return 0;
    }

    @Override
    public int getOnlineStoreUsage(int batchNumber) {
        String sql = """
        SELECT COALESCE(SUM(osi.quantity_available), 0) as `usage`
        FROM online_store_inventory osi
        WHERE osi.main_inventory_id = ?
    """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, batchNumber);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("usage");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting online store usage", e);
        }

        return 0;
    }

    @Override
    public boolean batchHasBeenSold(int batchNumber) {
        String sql = """
        SELECT COUNT(*) as `sale_count`
        FROM bill_item bi
        WHERE bi.main_inventory_id = ?
    """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, batchNumber);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("sale_count") > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error checking batch sales history", e);
        }

        return false;
    }

    @Override
    public List<MainInventory> findLowStockBatches(int threshold) {
        String sql = """
            SELECT mi.main_inventory_id, mi.product_code, mi.quantity_received,
                   mi.purchase_price, mi.purchase_date, mi.expiry_date,
                   mi.supplier_name, mi.remaining_quantity
            FROM main_inventory mi
            WHERE mi.remaining_quantity > 0 AND mi.remaining_quantity < ?
            ORDER BY mi.remaining_quantity ASC, mi.expiry_date ASC
        """;

        List<MainInventory> lowStockBatches = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, threshold);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                lowStockBatches.add(mapResultSetToMainInventory(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding low stock batches", e);
        }

        return lowStockBatches;
    }

    @Override
    public List<MainInventory> findBatchesExpiringBefore(LocalDate beforeDate) {
        String sql = """
            SELECT mi.main_inventory_id, mi.product_code, mi.quantity_received,
                   mi.purchase_price, mi.purchase_date, mi.expiry_date,
                   mi.supplier_name, mi.remaining_quantity
            FROM main_inventory mi
            WHERE mi.expiry_date IS NOT NULL 
            AND mi.expiry_date <= ? 
            AND mi.remaining_quantity > 0
            ORDER BY mi.expiry_date ASC
        """;

        List<MainInventory> expiringBatches = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(beforeDate));
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                expiringBatches.add(mapResultSetToMainInventory(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding expiring batches", e);
        }

        return expiringBatches;
    }

    @Override
    public List<CategoryData> findAllCategories() {
        String sql = """
            SELECT category_id, category_name, category_code 
            FROM category 
            ORDER BY category_name
        """;

        List<CategoryData> categories = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                categories.add(new CategoryData(
                        rs.getInt("category_id"),
                        rs.getString("category_name"),
                        rs.getString("category_code")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding all categories", e);
        }

        return categories;
    }

    @Override
    public List<SubcategoryData> findSubcategoriesByCategory(int categoryId) {
        String sql = """
            SELECT subcategory_id, subcategory_name, subcategory_code, category_id
            FROM subcategory 
            WHERE category_id = ?
            ORDER BY subcategory_name
        """;

        List<SubcategoryData> subcategories = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, categoryId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                subcategories.add(new SubcategoryData(
                        rs.getInt("subcategory_id"),
                        rs.getString("subcategory_name"),
                        rs.getString("subcategory_code"),
                        rs.getInt("category_id")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding subcategories by category", e);
        }

        return subcategories;
    }

    @Override
    public List<BrandData> findAllBrands() {
        String sql = """
            SELECT brand_id, brand_name, brand_code 
            FROM brand 
            ORDER BY brand_name
        """;

        List<BrandData> brands = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                brands.add(new BrandData(
                        rs.getInt("brand_id"),
                        rs.getString("brand_name"),
                        rs.getString("brand_code")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding all brands", e);
        }

        return brands;
    }

    // ==================== HELPER METHOD ====================

    private MainInventory mapResultSetToMainInventory(ResultSet rs) throws SQLException {
        return new MainInventory(
                rs.getInt("main_inventory_id"),
                new ProductCode(rs.getString("product_code")),
                rs.getInt("quantity_received"),
                new Money(rs.getBigDecimal("purchase_price")),
                rs.getDate("purchase_date").toLocalDate(),
                rs.getDate("expiry_date") != null ? rs.getDate("expiry_date").toLocalDate() : null,
                rs.getString("supplier_name"),
                rs.getInt("remaining_quantity")
        );
    }
}