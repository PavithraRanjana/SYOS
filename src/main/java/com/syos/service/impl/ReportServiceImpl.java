package com.syos.service.impl;

import com.syos.repository.interfaces.ReportRepository;
import com.syos.service.interfaces.ReportService;
import java.time.LocalDate;
import java.util.List;
import java.math.BigDecimal;

/**
 * Implementation of ReportService that orchestrates report generation.
 * Follows Dependency Inversion Principle - depends on ReportRepository abstraction.
 * Each method implements one specific report generation (Single Responsibility).
 */
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;

    public ReportServiceImpl(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    @Override
    public DailySalesReport generateDailySalesReport(LocalDate reportDate) {
        try {
            // Fetch physical store sales data
            List<SalesItem> physicalSalesItems = reportRepository.getPhysicalStoreSalesData(reportDate);
            BigDecimal physicalRevenue = reportRepository.getPhysicalStoreRevenue(reportDate);
            PhysicalStoreSales physicalStoreSales = new PhysicalStoreSales(physicalSalesItems, physicalRevenue);

            // Fetch online store sales data
            List<SalesItem> onlineSalesItems = reportRepository.getOnlineStoreSalesData(reportDate);
            BigDecimal onlineRevenue = reportRepository.getOnlineStoreRevenue(reportDate);
            OnlineStoreSales onlineStoreSales = new OnlineStoreSales(onlineSalesItems, onlineRevenue);

            // Calculate total revenue
            BigDecimal totalRevenue = physicalRevenue.add(onlineRevenue);

            return new DailySalesReport(reportDate, physicalStoreSales, onlineStoreSales, totalRevenue);

        } catch (Exception e) {
            throw new ReportGenerationException("Failed to generate daily sales report for " + reportDate, e);
        }
    }

    @Override
    public RestockReport generateRestockReport(LocalDate reportDate) {
        try {
            // Fetch restock needs for both stores
            List<RestockItem> physicalStoreItems = reportRepository.getPhysicalStoreRestockNeeds(reportDate);
            List<RestockItem> onlineStoreItems = reportRepository.getOnlineStoreRestockNeeds(reportDate);

            return new RestockReport(reportDate, physicalStoreItems, onlineStoreItems);

        } catch (Exception e) {
            throw new ReportGenerationException("Failed to generate restock report for " + reportDate, e);
        }
    }

    @Override
    public ReorderReport generateReorderReport(LocalDate reportDate) {
        try {
            // Fetch items that need reordering from main inventory
            List<ReorderItem> reorderItems = reportRepository.getMainInventoryReorderNeeds(reportDate);

            return new ReorderReport(reportDate, reorderItems);

        } catch (Exception e) {
            throw new ReportGenerationException("Failed to generate reorder report for " + reportDate, e);
        }
    }

    @Override
    public StockReport generateStockReport(LocalDate reportDate) {
        try {
            // Fetch comprehensive stock data
            List<StockItem> stockItems = reportRepository.getStockReportData(reportDate);

            return new StockReport(reportDate, stockItems);

        } catch (Exception e) {
            throw new ReportGenerationException("Failed to generate stock report for " + reportDate, e);
        }
    }

    @Override
    public BillReport generateBillReport(LocalDate reportDate) {
        try {
            // Fetch bill data for both stores
            List<BillSummary> physicalStoreBills = reportRepository.getPhysicalStoreBills(reportDate);
            List<BillSummary> onlineStoreBills = reportRepository.getOnlineStoreBills(reportDate);

            // Get transaction counts
            int physicalTransactionCount = reportRepository.getPhysicalStoreTransactionCount(reportDate);
            int onlineTransactionCount = reportRepository.getOnlineStoreTransactionCount(reportDate);

            return new BillReport(reportDate, physicalStoreBills, onlineStoreBills,
                    physicalTransactionCount, onlineTransactionCount);

        } catch (Exception e) {
            throw new ReportGenerationException("Failed to generate bill report for " + reportDate, e);
        }
    }

    /**
     * Custom exception for report generation failures.
     */
    public static class ReportGenerationException extends RuntimeException {
        public ReportGenerationException(String message, Throwable cause) {
            super(message, cause);
        }

        public ReportGenerationException(String message) {
            super(message);
        }
    }
}