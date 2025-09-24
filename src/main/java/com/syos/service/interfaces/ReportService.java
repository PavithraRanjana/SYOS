package com.syos.service.interfaces;

import java.time.LocalDate;

/**
 * Service interface for generating various reports in the SYOS system.
 * Follows Single Responsibility Principle - each method generates one specific report.
 */
public interface ReportService {

    /**
     * Generates daily sales report for a given date.
     * Shows sales data separated by physical and online stores.
     *
     * @param reportDate Date for which to generate the report
     * @return DailySalesReport containing all sales data
     */
    DailySalesReport generateDailySalesReport(LocalDate reportDate);

    /**
     * Generates restock report showing items needed to maintain 70 units
     * in each store inventory.
     *
     * @param reportDate Date for which to check inventory levels
     * @return RestockReport containing items that need restocking
     */
    RestockReport generateRestockReport(LocalDate reportDate);

    /**
     * Generates reorder report for main inventory items below 50 units.
     *
     * @param reportDate Date for which to check stock levels
     * @return ReorderReport containing items that need reordering
     */
    ReorderReport generateReorderReport(LocalDate reportDate);

    /**
     * Generates comprehensive stock report showing batch-wise details
     * across all inventories.
     *
     * @param reportDate Date up to which to show stock details
     * @return StockReport containing batch-wise inventory data
     */
    StockReport generateStockReport(LocalDate reportDate);

    /**
     * Generates bill report showing all transactions for a given date.
     *
     * @param reportDate Date for which to show transactions
     * @return BillReport containing all bill transactions
     */
    BillReport generateBillReport(LocalDate reportDate);

    // ==================== REPORT DATA TRANSFER OBJECTS ====================

    /**
     * Daily Sales Report DTO.
     */
    class DailySalesReport {
        private final LocalDate reportDate;
        private final PhysicalStoreSales physicalStoreSales;
        private final OnlineStoreSales onlineStoreSales;
        private final java.math.BigDecimal totalRevenue;

        public DailySalesReport(LocalDate reportDate, PhysicalStoreSales physicalStoreSales,
                                OnlineStoreSales onlineStoreSales, java.math.BigDecimal totalRevenue) {
            this.reportDate = reportDate;
            this.physicalStoreSales = physicalStoreSales;
            this.onlineStoreSales = onlineStoreSales;
            this.totalRevenue = totalRevenue;
        }

        public LocalDate getReportDate() { return reportDate; }
        public PhysicalStoreSales getPhysicalStoreSales() { return physicalStoreSales; }
        public OnlineStoreSales getOnlineStoreSales() { return onlineStoreSales; }
        public java.math.BigDecimal getTotalRevenue() { return totalRevenue; }
    }

    /**
     * Physical Store Sales DTO.
     */
    class PhysicalStoreSales {
        private final java.util.List<SalesItem> items;
        private final java.math.BigDecimal revenue;

        public PhysicalStoreSales(java.util.List<SalesItem> items, java.math.BigDecimal revenue) {
            this.items = items;
            this.revenue = revenue;
        }

        public java.util.List<SalesItem> getItems() { return items; }
        public java.math.BigDecimal getRevenue() { return revenue; }
    }

    /**
     * Online Store Sales DTO.
     */
    class OnlineStoreSales {
        private final java.util.List<SalesItem> items;
        private final java.math.BigDecimal revenue;

        public OnlineStoreSales(java.util.List<SalesItem> items, java.math.BigDecimal revenue) {
            this.items = items;
            this.revenue = revenue;
        }

        public java.util.List<SalesItem> getItems() { return items; }
        public java.math.BigDecimal getRevenue() { return revenue; }
    }

    /**
     * Sales Item DTO for daily sales report.
     */
    class SalesItem {
        private final String productCode;
        private final String productName;
        private final int totalQuantity;
        private final java.math.BigDecimal revenue;

        public SalesItem(String productCode, String productName, int totalQuantity, java.math.BigDecimal revenue) {
            this.productCode = productCode;
            this.productName = productName;
            this.totalQuantity = totalQuantity;
            this.revenue = revenue;
        }

        public String getProductCode() { return productCode; }
        public String getProductName() { return productName; }
        public int getTotalQuantity() { return totalQuantity; }
        public java.math.BigDecimal getRevenue() { return revenue; }
    }

    /**
     * Restock Report DTO.
     */
    class RestockReport {
        private final LocalDate reportDate;
        private final java.util.List<RestockItem> physicalStoreItems;
        private final java.util.List<RestockItem> onlineStoreItems;

        public RestockReport(LocalDate reportDate, java.util.List<RestockItem> physicalStoreItems,
                             java.util.List<RestockItem> onlineStoreItems) {
            this.reportDate = reportDate;
            this.physicalStoreItems = physicalStoreItems;
            this.onlineStoreItems = onlineStoreItems;
        }

        public LocalDate getReportDate() { return reportDate; }
        public java.util.List<RestockItem> getPhysicalStoreItems() { return physicalStoreItems; }
        public java.util.List<RestockItem> getOnlineStoreItems() { return onlineStoreItems; }
    }

    /**
     * Restock Item DTO.
     */
    class RestockItem {
        private final String productCode;
        private final String productName;
        private final int currentQuantity;
        private final int quantityNeeded;

        public RestockItem(String productCode, String productName, int currentQuantity, int quantityNeeded) {
            this.productCode = productCode;
            this.productName = productName;
            this.currentQuantity = currentQuantity;
            this.quantityNeeded = quantityNeeded;
        }

        public String getProductCode() { return productCode; }
        public String getProductName() { return productName; }
        public int getCurrentQuantity() { return currentQuantity; }
        public int getQuantityNeeded() { return quantityNeeded; }
    }

    /**
     * Reorder Report DTO.
     */
    class ReorderReport {
        private final LocalDate reportDate;
        private final java.util.List<ReorderItem> items;

        public ReorderReport(LocalDate reportDate, java.util.List<ReorderItem> items) {
            this.reportDate = reportDate;
            this.items = items;
        }

        public LocalDate getReportDate() { return reportDate; }
        public java.util.List<ReorderItem> getItems() { return items; }
    }

    /**
     * Reorder Item DTO.
     */
    class ReorderItem {
        private final String productCode;
        private final String productName;
        private final int totalQuantityAvailable;
        private final String status; // "CRITICAL" or "LOW"

        public ReorderItem(String productCode, String productName, int totalQuantityAvailable, String status) {
            this.productCode = productCode;
            this.productName = productName;
            this.totalQuantityAvailable = totalQuantityAvailable;
            this.status = status;
        }

        public String getProductCode() { return productCode; }
        public String getProductName() { return productName; }
        public int getTotalQuantityAvailable() { return totalQuantityAvailable; }
        public String getStatus() { return status; }
    }

    /**
     * Stock Report DTO.
     */
    class StockReport {
        private final LocalDate reportDate;
        private final java.util.List<StockItem> stockItems;

        public StockReport(LocalDate reportDate, java.util.List<StockItem> stockItems) {
            this.reportDate = reportDate;
            this.stockItems = stockItems;
        }

        public LocalDate getReportDate() { return reportDate; }
        public java.util.List<StockItem> getStockItems() { return stockItems; }
    }

    /**
     * Stock Item DTO for batch-wise stock report.
     */
    class StockItem {
        private final String productCode;
        private final String productName;
        private final int batchNumber;
        private final int quantityReceived;
        private final LocalDate purchaseDate;
        private final LocalDate expiryDate;
        private final int mainInventoryRemaining;
        private final int physicalStoreQuantity;
        private final int onlineStoreQuantity;

        public StockItem(String productCode, String productName, int batchNumber, int quantityReceived,
                         LocalDate purchaseDate, LocalDate expiryDate, int mainInventoryRemaining,
                         int physicalStoreQuantity, int onlineStoreQuantity) {
            this.productCode = productCode;
            this.productName = productName;
            this.batchNumber = batchNumber;
            this.quantityReceived = quantityReceived;
            this.purchaseDate = purchaseDate;
            this.expiryDate = expiryDate;
            this.mainInventoryRemaining = mainInventoryRemaining;
            this.physicalStoreQuantity = physicalStoreQuantity;
            this.onlineStoreQuantity = onlineStoreQuantity;
        }

        public String getProductCode() { return productCode; }
        public String getProductName() { return productName; }
        public int getBatchNumber() { return batchNumber; }
        public int getQuantityReceived() { return quantityReceived; }
        public LocalDate getPurchaseDate() { return purchaseDate; }
        public LocalDate getExpiryDate() { return expiryDate; }
        public int getMainInventoryRemaining() { return mainInventoryRemaining; }
        public int getPhysicalStoreQuantity() { return physicalStoreQuantity; }
        public int getOnlineStoreQuantity() { return onlineStoreQuantity; }
        public int getTotalQuantity() {
            return mainInventoryRemaining + physicalStoreQuantity + onlineStoreQuantity;
        }
    }

    /**
     * Bill Report DTO.
     */
    class BillReport {
        private final LocalDate reportDate;
        private final java.util.List<BillSummary> physicalStoreBills;
        private final java.util.List<BillSummary> onlineStoreBills;
        private final int totalPhysicalTransactions;
        private final int totalOnlineTransactions;

        public BillReport(LocalDate reportDate, java.util.List<BillSummary> physicalStoreBills,
                          java.util.List<BillSummary> onlineStoreBills, int totalPhysicalTransactions,
                          int totalOnlineTransactions) {
            this.reportDate = reportDate;
            this.physicalStoreBills = physicalStoreBills;
            this.onlineStoreBills = onlineStoreBills;
            this.totalPhysicalTransactions = totalPhysicalTransactions;
            this.totalOnlineTransactions = totalOnlineTransactions;
        }

        public LocalDate getReportDate() { return reportDate; }
        public java.util.List<BillSummary> getPhysicalStoreBills() { return physicalStoreBills; }
        public java.util.List<BillSummary> getOnlineStoreBills() { return onlineStoreBills; }
        public int getTotalPhysicalTransactions() { return totalPhysicalTransactions; }
        public int getTotalOnlineTransactions() { return totalOnlineTransactions; }
        public int getTotalTransactions() { return totalPhysicalTransactions + totalOnlineTransactions; }
    }

    /**
     * Bill Summary DTO.
     */
    class BillSummary {
        private final String billSerialNumber;
        private final LocalDate billDate;
        private final String customerName; // null for walk-in customers
        private final int itemCount;
        private final java.math.BigDecimal totalAmount;
        private final String transactionType;

        public BillSummary(String billSerialNumber, LocalDate billDate, String customerName,
                           int itemCount, java.math.BigDecimal totalAmount, String transactionType) {
            this.billSerialNumber = billSerialNumber;
            this.billDate = billDate;
            this.customerName = customerName;
            this.itemCount = itemCount;
            this.totalAmount = totalAmount;
            this.transactionType = transactionType;
        }

        public String getBillSerialNumber() { return billSerialNumber; }
        public LocalDate getBillDate() { return billDate; }
        public String getCustomerName() { return customerName; }
        public int getItemCount() { return itemCount; }
        public java.math.BigDecimal getTotalAmount() { return totalAmount; }
        public String getTransactionType() { return transactionType; }
    }
}