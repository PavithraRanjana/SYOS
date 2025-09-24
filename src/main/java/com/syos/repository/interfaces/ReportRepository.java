package com.syos.repository.interfaces;

import com.syos.service.interfaces.ReportService.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for fetching report data.
 * Follows Interface Segregation Principle - focused specifically on reporting queries.
 */
public interface ReportRepository {

    // ==================== DAILY SALES REPORT DATA ====================

    /**
     * Gets sales data for physical store on a specific date.
     *
     * @param reportDate Date to get sales for
     * @return List of sales items for physical store
     */
    List<SalesItem> getPhysicalStoreSalesData(LocalDate reportDate);

    /**
     * Gets sales data for online store on a specific date.
     *
     * @param reportDate Date to get sales for
     * @return List of sales items for online store
     */
    List<SalesItem> getOnlineStoreSalesData(LocalDate reportDate);

    /**
     * Gets total revenue for physical store on a specific date.
     *
     * @param reportDate Date to get revenue for
     * @return Total revenue amount
     */
    java.math.BigDecimal getPhysicalStoreRevenue(LocalDate reportDate);

    /**
     * Gets total revenue for online store on a specific date.
     *
     * @param reportDate Date to get revenue for
     * @return Total revenue amount
     */
    java.math.BigDecimal getOnlineStoreRevenue(LocalDate reportDate);

    // ==================== RESTOCK REPORT DATA ====================

    /**
     * Gets items in physical store that need restocking to reach 70 units.
     *
     * @param reportDate Date to check inventory levels
     * @return List of items needing restock for physical store
     */
    List<RestockItem> getPhysicalStoreRestockNeeds(LocalDate reportDate);

    /**
     * Gets items in online store that need restocking to reach 70 units.
     *
     * @param reportDate Date to check inventory levels
     * @return List of items needing restock for online store
     */
    List<RestockItem> getOnlineStoreRestockNeeds(LocalDate reportDate);

    // ==================== REORDER REPORT DATA ====================

    /**
     * Gets products in main inventory with total quantity below 50 units.
     *
     * @param reportDate Date to check stock levels
     * @return List of items needing reorder
     */
    List<ReorderItem> getMainInventoryReorderNeeds(LocalDate reportDate);

    // ==================== STOCK REPORT DATA ====================

    /**
     * Gets comprehensive stock data batch-wise up to a specific date.
     *
     * @param reportDate Date up to which to get stock data
     * @return List of stock items with batch details
     */
    List<StockItem> getStockReportData(LocalDate reportDate);

    // ==================== BILL REPORT DATA ====================

    /**
     * Gets bill summaries for physical store transactions on a specific date.
     *
     * @param reportDate Date to get bills for
     * @return List of physical store bill summaries
     */
    List<BillSummary> getPhysicalStoreBills(LocalDate reportDate);

    /**
     * Gets bill summaries for online store transactions on a specific date.
     *
     * @param reportDate Date to get bills for
     * @return List of online store bill summaries
     */
    List<BillSummary> getOnlineStoreBills(LocalDate reportDate);

    /**
     * Gets count of physical store transactions on a specific date.
     *
     * @param reportDate Date to count transactions for
     * @return Number of physical store transactions
     */
    int getPhysicalStoreTransactionCount(LocalDate reportDate);

    /**
     * Gets count of online store transactions on a specific date.
     *
     * @param reportDate Date to count transactions for
     * @return Number of online store transactions
     */
    int getOnlineStoreTransactionCount(LocalDate reportDate);
}