package com.syos.service.impl;

import com.syos.repository.interfaces.ReportRepository;
import com.syos.service.interfaces.ReportService;
import com.syos.service.interfaces.ReportService.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for ReportService implementation.
 * Tests all report generation methods with various scenarios.
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    private ReportService reportService;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        reportService = new ReportServiceImpl(reportRepository);
        testDate = LocalDate.of(2024, 9, 24);
    }

    // ==================== DAILY SALES REPORT TESTS ====================

    @Test
    @DisplayName("Should generate daily sales report with both store sales")
    void shouldGenerateDailySalesReportWithBothStoreSales() {
        // Arrange
        List<SalesItem> physicalSalesItems = Arrays.asList(
                new SalesItem("BVEDRB001", "Red Bull Energy Drink 250ml", 10, new BigDecimal("2500.00")),
                new SalesItem("BVSDCC001", "Coca-Cola 330ml", 15, new BigDecimal("1800.00"))
        );
        List<SalesItem> onlineSalesItems = Arrays.asList(
                new SalesItem("CHDKLIN001", "Lindt Dark Chocolate 70% 100g", 5, new BigDecimal("4250.00"))
        );

        BigDecimal physicalRevenue = new BigDecimal("4300.00");
        BigDecimal onlineRevenue = new BigDecimal("4250.00");

        when(reportRepository.getPhysicalStoreSalesData(testDate)).thenReturn(physicalSalesItems);
        when(reportRepository.getOnlineStoreSalesData(testDate)).thenReturn(onlineSalesItems);
        when(reportRepository.getPhysicalStoreRevenue(testDate)).thenReturn(physicalRevenue);
        when(reportRepository.getOnlineStoreRevenue(testDate)).thenReturn(onlineRevenue);

        // Act
        DailySalesReport report = reportService.generateDailySalesReport(testDate);

        // Assert
        assertNotNull(report);
        assertEquals(testDate, report.getReportDate());
        assertEquals(new BigDecimal("8550.00"), report.getTotalRevenue());

        PhysicalStoreSales physicalSales = report.getPhysicalStoreSales();
        assertEquals(2, physicalSales.getItems().size());
        assertEquals(physicalRevenue, physicalSales.getRevenue());

        OnlineStoreSales onlineSales = report.getOnlineStoreSales();
        assertEquals(1, onlineSales.getItems().size());
        assertEquals(onlineRevenue, onlineSales.getRevenue());

        // Verify repository calls
        verify(reportRepository).getPhysicalStoreSalesData(testDate);
        verify(reportRepository).getOnlineStoreSalesData(testDate);
        verify(reportRepository).getPhysicalStoreRevenue(testDate);
        verify(reportRepository).getOnlineStoreRevenue(testDate);
    }

    @Test
    @DisplayName("Should handle daily sales report with no sales")
    void shouldHandleDailySalesReportWithNoSales() {
        // Arrange
        when(reportRepository.getPhysicalStoreSalesData(testDate)).thenReturn(Collections.emptyList());
        when(reportRepository.getOnlineStoreSalesData(testDate)).thenReturn(Collections.emptyList());
        when(reportRepository.getPhysicalStoreRevenue(testDate)).thenReturn(BigDecimal.ZERO);
        when(reportRepository.getOnlineStoreRevenue(testDate)).thenReturn(BigDecimal.ZERO);

        // Act
        DailySalesReport report = reportService.generateDailySalesReport(testDate);

        // Assert
        assertNotNull(report);
        assertEquals(testDate, report.getReportDate());
        assertEquals(BigDecimal.ZERO, report.getTotalRevenue());
        assertTrue(report.getPhysicalStoreSales().getItems().isEmpty());
        assertTrue(report.getOnlineStoreSales().getItems().isEmpty());
    }

    @Test
    @DisplayName("Should throw exception when repository fails during sales report generation")
    void shouldThrowExceptionWhenRepositoryFailsDuringSalesReportGeneration() {
        // Arrange
        when(reportRepository.getPhysicalStoreSalesData(testDate))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ReportServiceImpl.ReportGenerationException.class, () -> {
            reportService.generateDailySalesReport(testDate);
        });

        verify(reportRepository).getPhysicalStoreSalesData(testDate);
    }

    // ==================== RESTOCK REPORT TESTS ====================

    @Test
    @DisplayName("Should generate restock report with items needing restocking")
    void shouldGenerateRestockReportWithItemsNeedingRestocking() {
        // Arrange
        List<RestockItem> physicalItems = Arrays.asList(
                new RestockItem("BVEDRB001", "Red Bull Energy Drink 250ml", 45, 25),
                new RestockItem("BVSDCC001", "Coca-Cola 330ml", 60, 10)
        );
        List<RestockItem> onlineItems = Arrays.asList(
                new RestockItem("CHDKLIN001", "Lindt Dark Chocolate 70% 100g", 30, 40)
        );

        when(reportRepository.getPhysicalStoreRestockNeeds(testDate)).thenReturn(physicalItems);
        when(reportRepository.getOnlineStoreRestockNeeds(testDate)).thenReturn(onlineItems);

        // Act
        RestockReport report = reportService.generateRestockReport(testDate);

        // Assert
        assertNotNull(report);
        assertEquals(testDate, report.getReportDate());
        assertEquals(2, report.getPhysicalStoreItems().size());
        assertEquals(1, report.getOnlineStoreItems().size());

        // Verify specific items
        RestockItem firstPhysicalItem = report.getPhysicalStoreItems().get(0);
        assertEquals("BVEDRB001", firstPhysicalItem.getProductCode());
        assertEquals(45, firstPhysicalItem.getCurrentQuantity());
        assertEquals(25, firstPhysicalItem.getQuantityNeeded());

        verify(reportRepository).getPhysicalStoreRestockNeeds(testDate);
        verify(reportRepository).getOnlineStoreRestockNeeds(testDate);
    }

    @Test
    @DisplayName("Should handle restock report with no items needing restocking")
    void shouldHandleRestockReportWithNoItemsNeedingRestocking() {
        // Arrange
        when(reportRepository.getPhysicalStoreRestockNeeds(testDate)).thenReturn(Collections.emptyList());
        when(reportRepository.getOnlineStoreRestockNeeds(testDate)).thenReturn(Collections.emptyList());

        // Act
        RestockReport report = reportService.generateRestockReport(testDate);

        // Assert
        assertNotNull(report);
        assertEquals(testDate, report.getReportDate());
        assertTrue(report.getPhysicalStoreItems().isEmpty());
        assertTrue(report.getOnlineStoreItems().isEmpty());
    }

    // ==================== REORDER REPORT TESTS ====================

    @Test
    @DisplayName("Should generate reorder report with items below 50 units")
    void shouldGenerateReorderReportWithItemsBelow50Units() {
        // Arrange
        List<ReorderItem> reorderItems = Arrays.asList(
                new ReorderItem("BVEDRB001", "Red Bull Energy Drink 250ml", 0, "CRITICAL"),
                new ReorderItem("BVSDCC001", "Coca-Cola 330ml", 25, "LOW"),
                new ReorderItem("CHDKLIN001", "Lindt Dark Chocolate 70% 100g", 10, "LOW")
        );

        when(reportRepository.getMainInventoryReorderNeeds(testDate)).thenReturn(reorderItems);

        // Act
        ReorderReport report = reportService.generateReorderReport(testDate);

        // Assert
        assertNotNull(report);
        assertEquals(testDate, report.getReportDate());
        assertEquals(3, report.getItems().size());

        // Verify critical and low items
        ReorderItem criticalItem = report.getItems().stream()
                .filter(item -> "CRITICAL".equals(item.getStatus()))
                .findFirst()
                .orElse(null);
        assertNotNull(criticalItem);
        assertEquals("BVEDRB001", criticalItem.getProductCode());
        assertEquals(0, criticalItem.getTotalQuantityAvailable());

        long lowCount = report.getItems().stream()
                .filter(item -> "LOW".equals(item.getStatus()))
                .count();
        assertEquals(2, lowCount);

        verify(reportRepository).getMainInventoryReorderNeeds(testDate);
    }

    @Test
    @DisplayName("Should handle reorder report with no items needing reorder")
    void shouldHandleReorderReportWithNoItemsNeedingReorder() {
        // Arrange
        when(reportRepository.getMainInventoryReorderNeeds(testDate)).thenReturn(Collections.emptyList());

        // Act
        ReorderReport report = reportService.generateReorderReport(testDate);

        // Assert
        assertNotNull(report);
        assertEquals(testDate, report.getReportDate());
        assertTrue(report.getItems().isEmpty());
    }

    // ==================== STOCK REPORT TESTS ====================

    @Test
    @DisplayName("Should generate comprehensive stock report")
    void shouldGenerateComprehensiveStockReport() {
        // Arrange
        List<StockItem> stockItems = Arrays.asList(
                new StockItem("BVEDRB001", "Red Bull Energy Drink 250ml", 1, 500,
                        LocalDate.of(2024, 9, 15), LocalDate.of(2025, 9, 15),
                        320, 100, 80),
                new StockItem("BVSDCC001", "Coca-Cola 330ml", 2, 1000,
                        LocalDate.of(2024, 9, 16), LocalDate.of(2025, 6, 16),
                        650, 200, 150)
        );

        when(reportRepository.getStockReportData(testDate)).thenReturn(stockItems);

        // Act
        StockReport report = reportService.generateStockReport(testDate);

        // Assert
        assertNotNull(report);
        assertEquals(testDate, report.getReportDate());
        assertEquals(2, report.getStockItems().size());

        // Verify first item details
        StockItem firstItem = report.getStockItems().get(0);
        assertEquals("BVEDRB001", firstItem.getProductCode());
        assertEquals(1, firstItem.getBatchNumber());
        assertEquals(500, firstItem.getQuantityReceived());
        assertEquals(500, firstItem.getTotalQuantity()); // 320 + 100 + 80
        assertNotNull(firstItem.getExpiryDate());

        verify(reportRepository).getStockReportData(testDate);
    }

    @Test
    @DisplayName("Should handle stock report with products having no batches")
    void shouldHandleStockReportWithProductsHavingNoBatches() {
        // Arrange
        List<StockItem> stockItems = Arrays.asList(
                new StockItem("NEWPRODUCT", "New Product", 0, 0, null, null, 0, 0, 0)
        );

        when(reportRepository.getStockReportData(testDate)).thenReturn(stockItems);

        // Act
        StockReport report = reportService.generateStockReport(testDate);

        // Assert
        assertNotNull(report);
        assertEquals(1, report.getStockItems().size());

        StockItem item = report.getStockItems().get(0);
        assertEquals("NEWPRODUCT", item.getProductCode());
        assertEquals(0, item.getBatchNumber());
        assertEquals(0, item.getTotalQuantity());
        assertNull(item.getPurchaseDate());
        assertNull(item.getExpiryDate());
    }

    // ==================== BILL REPORT TESTS ====================

    @Test
    @DisplayName("Should generate bill report with transactions from both stores")
    void shouldGenerateBillReportWithTransactionsFromBothStores() {
        // Arrange
        List<BillSummary> physicalBills = Arrays.asList(
                new BillSummary("BILL000001", testDate, null, 3, new BigDecimal("1250.00"), "CASH"),
                new BillSummary("BILL000002", testDate, null, 1, new BigDecimal("250.00"), "CASH")
        );
        List<BillSummary> onlineBills = Arrays.asList(
                new BillSummary("BILL000003", testDate, "John Doe", 2, new BigDecimal("1700.00"), "ONLINE")
        );

        when(reportRepository.getPhysicalStoreBills(testDate)).thenReturn(physicalBills);
        when(reportRepository.getOnlineStoreBills(testDate)).thenReturn(onlineBills);
        when(reportRepository.getPhysicalStoreTransactionCount(testDate)).thenReturn(2);
        when(reportRepository.getOnlineStoreTransactionCount(testDate)).thenReturn(1);

        // Act
        BillReport report = reportService.generateBillReport(testDate);

        // Assert
        assertNotNull(report);
        assertEquals(testDate, report.getReportDate());
        assertEquals(2, report.getPhysicalStoreBills().size());
        assertEquals(1, report.getOnlineStoreBills().size());
        assertEquals(2, report.getTotalPhysicalTransactions());
        assertEquals(1, report.getTotalOnlineTransactions());
        assertEquals(3, report.getTotalTransactions());

        // Verify specific bill details
        BillSummary physicalBill = report.getPhysicalStoreBills().get(0);
        assertEquals("BILL000001", physicalBill.getBillSerialNumber());
        assertEquals(3, physicalBill.getItemCount());
        assertEquals("CASH", physicalBill.getTransactionType());
        assertNull(physicalBill.getCustomerName()); // Walk-in customer

        BillSummary onlineBill = report.getOnlineStoreBills().get(0);
        assertEquals("John Doe", onlineBill.getCustomerName());
        assertEquals("ONLINE", onlineBill.getTransactionType());

        verify(reportRepository).getPhysicalStoreBills(testDate);
        verify(reportRepository).getOnlineStoreBills(testDate);
        verify(reportRepository).getPhysicalStoreTransactionCount(testDate);
        verify(reportRepository).getOnlineStoreTransactionCount(testDate);
    }

    @Test
    @DisplayName("Should handle bill report with no transactions")
    void shouldHandleBillReportWithNoTransactions() {
        // Arrange
        when(reportRepository.getPhysicalStoreBills(testDate)).thenReturn(Collections.emptyList());
        when(reportRepository.getOnlineStoreBills(testDate)).thenReturn(Collections.emptyList());
        when(reportRepository.getPhysicalStoreTransactionCount(testDate)).thenReturn(0);
        when(reportRepository.getOnlineStoreTransactionCount(testDate)).thenReturn(0);

        // Act
        BillReport report = reportService.generateBillReport(testDate);

        // Assert
        assertNotNull(report);
        assertEquals(testDate, report.getReportDate());
        assertTrue(report.getPhysicalStoreBills().isEmpty());
        assertTrue(report.getOnlineStoreBills().isEmpty());
        assertEquals(0, report.getTotalTransactions());
    }

    // ==================== ERROR HANDLING TESTS ====================

    @Test
    @DisplayName("Should throw ReportGenerationException when restock report fails")
    void shouldThrowReportGenerationExceptionWhenRestockReportFails() {
        // Arrange
        when(reportRepository.getPhysicalStoreRestockNeeds(testDate))
                .thenThrow(new RuntimeException("Database connection error"));

        // Act & Assert
        ReportServiceImpl.ReportGenerationException exception = assertThrows(
                ReportServiceImpl.ReportGenerationException.class, () -> {
                    reportService.generateRestockReport(testDate);
                });

        assertTrue(exception.getMessage().contains("Failed to generate restock report"));
        assertNotNull(exception.getCause());
    }

    @Test
    @DisplayName("Should throw ReportGenerationException when reorder report fails")
    void shouldThrowReportGenerationExceptionWhenReorderReportFails() {
        // Arrange
        when(reportRepository.getMainInventoryReorderNeeds(testDate))
                .thenThrow(new RuntimeException("SQL exception"));

        // Act & Assert
        ReportServiceImpl.ReportGenerationException exception = assertThrows(
                ReportServiceImpl.ReportGenerationException.class, () -> {
                    reportService.generateReorderReport(testDate);
                });

        assertTrue(exception.getMessage().contains("Failed to generate reorder report"));
    }

    @Test
    @DisplayName("Should throw ReportGenerationException when stock report fails")
    void shouldThrowReportGenerationExceptionWhenStockReportFails() {
        // Arrange
        when(reportRepository.getStockReportData(testDate))
                .thenThrow(new RuntimeException("Query timeout"));

        // Act & Assert
        ReportServiceImpl.ReportGenerationException exception = assertThrows(
                ReportServiceImpl.ReportGenerationException.class, () -> {
                    reportService.generateStockReport(testDate);
                });

        assertTrue(exception.getMessage().contains("Failed to generate stock report"));
    }

    @Test
    @DisplayName("Should throw ReportGenerationException when bill report fails")
    void shouldThrowReportGenerationExceptionWhenBillReportFails() {
        // Arrange
        when(reportRepository.getPhysicalStoreBills(testDate))
                .thenThrow(new RuntimeException("Table lock error"));

        // Act & Assert
        ReportServiceImpl.ReportGenerationException exception = assertThrows(
                ReportServiceImpl.ReportGenerationException.class, () -> {
                    reportService.generateBillReport(testDate);
                });

        assertTrue(exception.getMessage().contains("Failed to generate bill report"));
    }

    // ==================== INTEGRATION-STYLE TESTS ====================

    @Test
    @DisplayName("Should generate multiple reports for same date without interference")
    void shouldGenerateMultipleReportsForSameDateWithoutInterference() {
        // Arrange - Setup data for all reports
        when(reportRepository.getPhysicalStoreSalesData(testDate))
                .thenReturn(Arrays.asList(new SalesItem("TEST001", "Test Product", 5, BigDecimal.TEN)));
        when(reportRepository.getOnlineStoreSalesData(testDate))
                .thenReturn(Collections.emptyList());
        when(reportRepository.getPhysicalStoreRevenue(testDate))
                .thenReturn(BigDecimal.TEN);
        when(reportRepository.getOnlineStoreRevenue(testDate))
                .thenReturn(BigDecimal.ZERO);
        when(reportRepository.getPhysicalStoreRestockNeeds(testDate))
                .thenReturn(Arrays.asList(new RestockItem("TEST001", "Test Product", 30, 40)));
        when(reportRepository.getOnlineStoreRestockNeeds(testDate))
                .thenReturn(Collections.emptyList());
        when(reportRepository.getMainInventoryReorderNeeds(testDate))
                .thenReturn(Arrays.asList(new ReorderItem("TEST001", "Test Product", 25, "LOW")));

        // Act - Generate multiple reports
        DailySalesReport salesReport = reportService.generateDailySalesReport(testDate);
        RestockReport restockReport = reportService.generateRestockReport(testDate);
        ReorderReport reorderReport = reportService.generateReorderReport(testDate);

        // Assert - All reports should be generated successfully
        assertNotNull(salesReport);
        assertNotNull(restockReport);
        assertNotNull(reorderReport);

        // Verify all repository methods were called
        verify(reportRepository, times(1)).getPhysicalStoreSalesData(testDate);
        verify(reportRepository, times(1)).getPhysicalStoreRestockNeeds(testDate);
        verify(reportRepository, times(1)).getMainInventoryReorderNeeds(testDate);
    }

    @Test
    @DisplayName("Should handle edge case with future date gracefully")
    void shouldHandleEdgeCaseWithFutureDateGracefully() {
        // Arrange
        LocalDate futureDate = LocalDate.now().plusDays(30);
        when(reportRepository.getPhysicalStoreSalesData(futureDate))
                .thenReturn(Collections.emptyList());
        when(reportRepository.getOnlineStoreSalesData(futureDate))
                .thenReturn(Collections.emptyList());
        when(reportRepository.getPhysicalStoreRevenue(futureDate))
                .thenReturn(BigDecimal.ZERO);
        when(reportRepository.getOnlineStoreRevenue(futureDate))
                .thenReturn(BigDecimal.ZERO);

        // Act
        DailySalesReport report = reportService.generateDailySalesReport(futureDate);

        // Assert
        assertNotNull(report);
        assertEquals(futureDate, report.getReportDate());
        assertEquals(BigDecimal.ZERO, report.getTotalRevenue());
    }
}