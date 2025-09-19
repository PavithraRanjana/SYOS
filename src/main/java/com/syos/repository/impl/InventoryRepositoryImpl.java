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

public class InventoryRepositoryImpl implements InventoryRepository {
    private final Connection connection;

    public InventoryRepositoryImpl() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

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