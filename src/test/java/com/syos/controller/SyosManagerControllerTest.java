package com.syos.controller;

import com.syos.service.interfaces.ReportService;
import com.syos.service.interfaces.ReportService.*;
import com.syos.service.impl.ReportServiceImpl;
import com.syos.ui.interfaces.UserInterface;
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

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Complete unit tests for SyosManagerController.
 * Tests all major functionality with proper mock setup.
 */
@ExtendWith(MockitoExtension.class)
class SyosManagerControllerTest {

    @Mock
    private ReportService reportService;

    @Mock
    private UserInterface ui;

    private SyosManagerController controller;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        controller = new SyosManagerController(reportService, ui);
        testDate = LocalDate.of(2024, 9, 24);
    }

    // ==================== BASIC MENU TESTS ====================

    @Test
    @DisplayName("Should exit manager mode when option 6 is selected")
    void shouldExitManagerModeWhenOption6IsSelected() {
        // Arrange
        when(ui.getUserInput()).thenReturn("6");

        // Act
        controller.startManagerMode();

        // Assert
        verify(ui, atLeastOnce()).displaySuccess("Returning to main menu...");
    }

    @Test
    @DisplayName("Should handle invalid menu option gracefully")
    void shouldHandleInvalidMenuOptionGracefully() {
        // Arrange
        when(ui.getUserInput())
                .thenReturn("invalid")  // Invalid option
                .thenReturn("6");       // Exit

        // Act
        controller.startManagerMode();

        // Assert
        verify(ui).displayError("Invalid option. Please try again.");
        verify(ui).displaySuccess("Returning to main menu...");
    }

    @Test
    @DisplayName("Should handle unexpected exceptions during menu operation")
    void shouldHandleUnexpectedExceptionsDuringMenuOperation() {
        // Arrange
        when(ui.getUserInput())
                .thenThrow(new RuntimeException("UI error"))
                .thenReturn("6"); // Exit after error

        // Act
        controller.startManagerMode();

        // Assert
        verify(ui).displayError(contains("Unexpected error"));
    }

    // ==================== DAILY SALES REPORT TESTS ====================

    @Test
    @DisplayName("Should generate daily sales report with valid date")
    void shouldGenerateDailySalesReportWithValidDate() {
        // Arrange
        when(ui.getUserInput())
                .thenReturn("1")                    // Select daily sales report
                .thenReturn("2024-09-24")          // Valid date
                .thenReturn("")                     // Wait for enter
                .thenReturn("6");                   // Exit

        // Mock report data
        PhysicalStoreSales physicalSales = new PhysicalStoreSales(Collections.emptyList(), BigDecimal.ZERO);
        OnlineStoreSales onlineSales = new OnlineStoreSales(Collections.emptyList(), BigDecimal.ZERO);
        DailySalesReport report = new DailySalesReport(testDate, physicalSales, onlineSales, BigDecimal.ZERO);

        when(reportService.generateDailySalesReport(testDate)).thenReturn(report);

        // Act
        controller.startManagerMode();

        // Assert
        verify(reportService).generateDailySalesReport(testDate);
        verify(ui).waitForEnter();
    }

    @Test
    @DisplayName("Should handle today input for daily sales report")
    void shouldHandleTodayInputForDailySalesReport() {
        // Arrange
        when(ui.getUserInput())
                .thenReturn("1")                    // Select daily sales report
                .thenReturn("today")               // Use today
                .thenReturn("")                     // Wait for enter
                .thenReturn("6");                   // Exit

        PhysicalStoreSales physicalSales = new PhysicalStoreSales(Collections.emptyList(), BigDecimal.ZERO);
        OnlineStoreSales onlineSales = new OnlineStoreSales(Collections.emptyList(), BigDecimal.ZERO);
        DailySalesReport report = new DailySalesReport(LocalDate.now(), physicalSales, onlineSales, BigDecimal.ZERO);

        when(reportService.generateDailySalesReport(any(LocalDate.class))).thenReturn(report);

        // Act
        controller.startManagerMode();

        // Assert
        verify(reportService).generateDailySalesReport(any(LocalDate.class));
        verify(ui).waitForEnter();
    }

    @Test
    @DisplayName("Should handle daily sales report with actual data")
    void shouldHandleDailySalesReportWithActualData() {
        // Arrange
        when(ui.getUserInput())
                .thenReturn("1")                    // Select daily sales report
                .thenReturn("2024-09-24")          // Valid date
                .thenReturn("")                     // Wait for enter
                .thenReturn("6");                   // Exit

        // Create report with actual data
        var physicalItems = Arrays.asList(
                new SalesItem("BVEDRB001", "Red Bull Energy Drink 250ml", 10, new BigDecimal("2500.00"))
        );
        var onlineItems = Arrays.asList(
                new SalesItem("CHDKLIN001", "Lindt Dark Chocolate 70% 100g", 5, new BigDecimal("4250.00"))
        );

        PhysicalStoreSales physicalSales = new PhysicalStoreSales(physicalItems, new BigDecimal("2500.00"));
        OnlineStoreSales onlineSales = new OnlineStoreSales(onlineItems, new BigDecimal("4250.00"));
        DailySalesReport report = new DailySalesReport(testDate, physicalSales, onlineSales, new BigDecimal("6750.00"));

        when(reportService.generateDailySalesReport(testDate)).thenReturn(report);

        // Act
        controller.startManagerMode();

        // Assert
        verify(reportService).generateDailySalesReport(testDate);
        verify(ui).waitForEnter();
    }

    @Test
    @DisplayName("Should handle sales report generation failure")
    void shouldHandleSalesReportGenerationFailure() {
        // Arrange
        when(ui.getUserInput())
                .thenReturn("1")                    // Select daily sales report
                .thenReturn("2024-09-24")          // Valid date
                .thenReturn("")                     // Wait for enter
                .thenReturn("6");                   // Exit

        when(reportService.generateDailySalesReport(testDate))
                .thenThrow(new ReportServiceImpl.ReportGenerationException("Database error"));

        // Act
        controller.startManagerMode();

        // Assert
        verify(ui).displayError(contains("Failed to generate sales report"));
        verify(ui).waitForEnter();
    }

    // ==================== RESTOCK REPORT TESTS ====================

    @Test
    @DisplayName("Should generate restock report successfully")
    void shouldGenerateRestockReportSuccessfully() {
        // Arrange
        when(ui.getUserInput())
                .thenReturn("2")                    // Select restock report
                .thenReturn("2024-09-24")          // Valid date
                .thenReturn("")                     // Wait for enter
                .thenReturn("6");                   // Exit

        // Mock restock data
        var physicalItems = Arrays.asList(
                new RestockItem("BVEDRB001", "Red Bull Energy Drink 250ml", 45, 25)
        );
        var onlineItems = Arrays.asList(
                new RestockItem("CHDKLIN001", "Lindt Dark Chocolate 70% 100g", 30, 40)
        );

        RestockReport report = new RestockReport(testDate, physicalItems, onlineItems);
        when(reportService.generateRestockReport(testDate)).thenReturn(report);

        // Act
        controller.startManagerMode();

        // Assert
        verify(reportService).generateRestockReport(testDate);
        verify(ui).waitForEnter();
    }

    @Test
    @DisplayName("Should handle restock report with no items needing restocking")
    void shouldHandleRestockReportWithNoItemsNeedingRestocking() {
        // Arrange
        when(ui.getUserInput())
                .thenReturn("2")                    // Select restock report
                .thenReturn("2024-09-24")          // Valid date
                .thenReturn("")                     // Wait for enter
                .thenReturn("6");                   // Exit

        RestockReport report = new RestockReport(testDate, Collections.emptyList(), Collections.emptyList());
        when(reportService.generateRestockReport(testDate)).thenReturn(report);

        // Act
        controller.startManagerMode();

        // Assert
        verify(reportService).generateRestockReport(testDate);
        verify(ui).waitForEnter();
    }

    // ==================== REORDER REPORT TESTS ====================

    @Test
    @DisplayName("Should generate reorder report successfully")
    void shouldGenerateReorderReportSuccessfully() {
        // Arrange
        when(ui.getUserInput())
                .thenReturn("3")                    // Select reorder report
                .thenReturn("2024-09-24")          // Valid date
                .thenReturn("")                     // Wait for enter
                .thenReturn("6");                   // Exit

        // Mock reorder data
        var reorderItems = Arrays.asList(
                new ReorderItem("CRITICAL001", "Critical Product", 0, "CRITICAL"),
                new ReorderItem("LOW001", "Low Stock Product", 25, "LOW")
        );

        ReorderReport report = new ReorderReport(testDate, reorderItems);
        when(reportService.generateReorderReport(testDate)).thenReturn(report);

        // Act
        controller.startManagerMode();

        // Assert
        verify(reportService).generateReorderReport(testDate);
        verify(ui).waitForEnter();
    }

    @Test
    @DisplayName("Should handle reorder report with adequate stock levels")
    void shouldHandleReorderReportWithAdequateStockLevels() {
        // Arrange
        when(ui.getUserInput())
                .thenReturn("3")                    // Select reorder report
                .thenReturn("2024-09-24")          // Valid date
                .thenReturn("")                     // Wait for enter
                .thenReturn("6");                   // Exit

        ReorderReport report = new ReorderReport(testDate, Collections.emptyList());
        when(reportService.generateReorderReport(testDate)).thenReturn(report);

        // Act
        controller.startManagerMode();

        // Assert
        verify(reportService).generateReorderReport(testDate);
        verify(ui).waitForEnter();
    }

    // ==================== STOCK REPORT TESTS ====================

    @Test
    @DisplayName("Should generate stock report successfully")
    void shouldGenerateStockReportSuccessfully() {
        // Arrange
        when(ui.getUserInput())
                .thenReturn("4")                    // Select stock report
                .thenReturn("2024-09-24")          // Valid date
                .thenReturn("")                     // Wait for enter
                .thenReturn("6");                   // Exit

        // Mock stock data
        var stockItems = Arrays.asList(
                new StockItem("BVEDRB001", "Red Bull Energy Drink 250ml", 1, 500,
                        LocalDate.of(2024, 9, 15), LocalDate.of(2025, 9, 15),
                        320, 100, 80)
        );

        StockReport report = new StockReport(testDate, stockItems);
        when(reportService.generateStockReport(testDate)).thenReturn(report);

        // Act
        controller.startManagerMode();

        // Assert
        verify(reportService).generateStockReport(testDate);
        verify(ui).waitForEnter();
    }

    @Test
    @DisplayName("Should handle stock report with no stock data")
    void shouldHandleStockReportWithNoStockData() {
        // Arrange
        when(ui.getUserInput())
                .thenReturn("4")                    // Select stock report
                .thenReturn("2024-09-24")          // Valid date
                .thenReturn("")                     // Wait for enter
                .thenReturn("6");                   // Exit

        StockReport report = new StockReport(testDate, Collections.emptyList());
        when(reportService.generateStockReport(testDate)).thenReturn(report);

        // Act
        controller.startManagerMode();

        // Assert
        verify(reportService).generateStockReport(testDate);
        verify(ui).waitForEnter();
    }

    // ==================== BILL REPORT TESTS ====================

    @Test
    @DisplayName("Should generate bill report successfully")
    void shouldGenerateBillReportSuccessfully() {
        // Arrange
        when(ui.getUserInput())
                .thenReturn("5")                    // Select bill report
                .thenReturn("2024-09-24")          // Valid date
                .thenReturn("")                     // Wait for enter
                .thenReturn("6");                   // Exit

        // Mock bill data
        var physicalBills = Arrays.asList(
                new BillSummary("BILL000001", testDate, null, 3, new BigDecimal("1250.00"), "CASH")
        );
        var onlineBills = Arrays.asList(
                new BillSummary("BILL000002", testDate, "John Doe", 2, new BigDecimal("1700.00"), "ONLINE")
        );

        BillReport report = new BillReport(testDate, physicalBills, onlineBills, 1, 1);
        when(reportService.generateBillReport(testDate)).thenReturn(report);

        // Act
        controller.startManagerMode();

        // Assert
        verify(reportService).generateBillReport(testDate);
        verify(ui).waitForEnter();
    }

    @Test
    @DisplayName("Should handle bill report with no transactions")
    void shouldHandleBillReportWithNoTransactions() {
        // Arrange
        when(ui.getUserInput())
                .thenReturn("5")                    // Select bill report
                .thenReturn("2024-09-24")          // Valid date
                .thenReturn("")                     // Wait for enter
                .thenReturn("6");                   // Exit

        BillReport report = new BillReport(testDate, Collections.emptyList(), Collections.emptyList(), 0, 0);
        when(reportService.generateBillReport(testDate)).thenReturn(report);

        // Act
        controller.startManagerMode();

        // Assert
        verify(reportService).generateBillReport(testDate);
        verify(ui).waitForEnter();
    }

    // ==================== INPUT VALIDATION TESTS ====================

    @Test
    @DisplayName("Should handle invalid date format")
    void shouldHandleInvalidDateFormat() {
        // Arrange
        when(ui.getUserInput())
                .thenReturn("1")                    // Select daily sales report
                .thenReturn("invalid-date")        // Invalid date
                .thenReturn("today")               // Valid fallback
                .thenReturn("")                     // Wait for enter
                .thenReturn("6");                   // Exit

        PhysicalStoreSales physicalSales = new PhysicalStoreSales(Collections.emptyList(), BigDecimal.ZERO);
        OnlineStoreSales onlineSales = new OnlineStoreSales(Collections.emptyList(), BigDecimal.ZERO);
        DailySalesReport report = new DailySalesReport(LocalDate.now(), physicalSales, onlineSales, BigDecimal.ZERO);

        when(reportService.generateDailySalesReport(any(LocalDate.class))).thenReturn(report);

        // Act
        controller.startManagerMode();

        // Assert
        verify(ui).displayError("Invalid date format. Please use YYYY-MM-DD format.");
        verify(reportService).generateDailySalesReport(any(LocalDate.class));
    }

    @Test
    @DisplayName("Should reject future dates")
    void shouldRejectFutureDates() {
        // Arrange
        LocalDate futureDate = LocalDate.now().plusDays(30);

        when(ui.getUserInput())
                .thenReturn("1")                           // Select daily sales report
                .thenReturn(futureDate.toString())        // Future date
                .thenReturn("today")                      // Valid fallback
                .thenReturn("")                            // Wait for enter
                .thenReturn("6");                          // Exit

        PhysicalStoreSales physicalSales = new PhysicalStoreSales(Collections.emptyList(), BigDecimal.ZERO);
        OnlineStoreSales onlineSales = new OnlineStoreSales(Collections.emptyList(), BigDecimal.ZERO);
        DailySalesReport report = new DailySalesReport(LocalDate.now(), physicalSales, onlineSales, BigDecimal.ZERO);

        when(reportService.generateDailySalesReport(any(LocalDate.class))).thenReturn(report);

        // Act
        controller.startManagerMode();

        // Assert
        verify(ui).displayError("Date cannot be in the future. Please try again.");
        verify(reportService).generateDailySalesReport(any(LocalDate.class));
    }

    @Test
    @DisplayName("Should handle empty date input")
    void shouldHandleEmptyDateInput() {
        // Arrange
        when(ui.getUserInput())
                .thenReturn("1")                    // Select daily sales report
                .thenReturn("")                     // Empty date
                .thenReturn("2024-09-24")          // Valid date
                .thenReturn("")                     // Wait for enter
                .thenReturn("6");                   // Exit

        PhysicalStoreSales physicalSales = new PhysicalStoreSales(Collections.emptyList(), BigDecimal.ZERO);
        OnlineStoreSales onlineSales = new OnlineStoreSales(Collections.emptyList(), BigDecimal.ZERO);
        DailySalesReport report = new DailySalesReport(testDate, physicalSales, onlineSales, BigDecimal.ZERO);

        when(reportService.generateDailySalesReport(testDate)).thenReturn(report);

        // Act
        controller.startManagerMode();

        // Assert
        verify(ui).displayError("Date cannot be empty. Please try again.");
        verify(reportService).generateDailySalesReport(testDate);
    }

    @Test
    @DisplayName("Should handle null input gracefully")
    void shouldHandleNullInputGracefully() {
        // Arrange
        when(ui.getUserInput())
                .thenReturn("1")                    // Select daily sales report
                .thenReturn(null)                  // Null input
                .thenReturn("today")               // Valid fallback after error
                .thenReturn("")                     // Wait for enter
                .thenReturn("6");                   // Exit

        when(ui.confirmAction("Try again?")).thenReturn(true);

        PhysicalStoreSales physicalSales = new PhysicalStoreSales(Collections.emptyList(), BigDecimal.ZERO);
        OnlineStoreSales onlineSales = new OnlineStoreSales(Collections.emptyList(), BigDecimal.ZERO);
        DailySalesReport report = new DailySalesReport(LocalDate.now(), physicalSales, onlineSales, BigDecimal.ZERO);

        when(reportService.generateDailySalesReport(any(LocalDate.class))).thenReturn(report);

        // Act
        controller.startManagerMode();

        // Assert
        verify(ui).displayError("Invalid input received. Please try again.");
        verify(reportService).generateDailySalesReport(any(LocalDate.class));
    }

    @Test
    @DisplayName("Should handle date input cancellation")
    void shouldHandleDateInputCancellation() {
        // Arrange
        when(ui.getUserInput())
                .thenReturn("1")                    // Select daily sales report
                .thenReturn(null)                  // Null input triggers error
                .thenReturn("6");                   // Exit

        when(ui.confirmAction("Try again?")).thenReturn(false); // User cancels

        // Act
        controller.startManagerMode();

        // Assert
        verify(ui).confirmAction("Try again?");
        verify(reportService, never()).generateDailySalesReport(any(LocalDate.class));
    }

    // ==================== ERROR RECOVERY TESTS ====================

    @Test
    @DisplayName("Should recover from service exceptions and continue operation")
    void shouldRecoverFromServiceExceptionsAndContinueOperation() {
        // Arrange
        when(ui.getUserInput())
                .thenReturn("1")                    // Select daily sales report (fails)
                .thenReturn("2024-09-24")          // Date
                .thenReturn("")                     // Wait for enter
                .thenReturn("2")                    // Select restock report (succeeds)
                .thenReturn("2024-09-24")          // Date
                .thenReturn("")                     // Wait for enter
                .thenReturn("6");                   // Exit

        // First report fails, second succeeds
        when(reportService.generateDailySalesReport(testDate))
                .thenThrow(new ReportServiceImpl.ReportGenerationException("Service error"));

        RestockReport restockReport = new RestockReport(testDate, Collections.emptyList(), Collections.emptyList());
        when(reportService.generateRestockReport(testDate)).thenReturn(restockReport);

        // Act
        controller.startManagerMode();

        // Assert
        verify(ui).displayError(contains("Failed to generate sales report"));
        verify(reportService).generateRestockReport(testDate);
        verify(ui, times(2)).waitForEnter(); // Once for failed report, once for successful report
    }

    // ==================== INTEGRATION TESTS ====================

    @Test
    @DisplayName("Should handle multiple report generations in sequence")
    void shouldHandleMultipleReportGenerationsInSequence() {
        // Arrange
        when(ui.getUserInput())
                .thenReturn("1")                    // Daily sales report
                .thenReturn("2024-09-24")          // Date
                .thenReturn("")                     // Wait for enter
                .thenReturn("2")                    // Restock report
                .thenReturn("2024-09-24")          // Date
                .thenReturn("")                     // Wait for enter
                .thenReturn("3")                    // Reorder report
                .thenReturn("2024-09-24")          // Date
                .thenReturn("")                     // Wait for enter
                .thenReturn("6");                   // Exit

        // Mock all reports
        PhysicalStoreSales physicalSales = new PhysicalStoreSales(Collections.emptyList(), BigDecimal.ZERO);
        OnlineStoreSales onlineSales = new OnlineStoreSales(Collections.emptyList(), BigDecimal.ZERO);
        DailySalesReport salesReport = new DailySalesReport(testDate, physicalSales, onlineSales, BigDecimal.ZERO);
        RestockReport restockReport = new RestockReport(testDate, Collections.emptyList(), Collections.emptyList());
        ReorderReport reorderReport = new ReorderReport(testDate, Collections.emptyList());

        when(reportService.generateDailySalesReport(testDate)).thenReturn(salesReport);
        when(reportService.generateRestockReport(testDate)).thenReturn(restockReport);
        when(reportService.generateReorderReport(testDate)).thenReturn(reorderReport);

        // Act
        controller.startManagerMode();

        // Assert
        verify(reportService).generateDailySalesReport(testDate);
        verify(reportService).generateRestockReport(testDate);
        verify(reportService).generateReorderReport(testDate);
        verify(ui, times(3)).waitForEnter(); // Called three times
    }

    @Test
    @DisplayName("Should handle all report types in single session")
    void shouldHandleAllReportTypesInSingleSession() {
        // Arrange
        when(ui.getUserInput())
                .thenReturn("1").thenReturn("today").thenReturn("")     // Daily sales
                .thenReturn("2").thenReturn("today").thenReturn("")     // Restock
                .thenReturn("3").thenReturn("today").thenReturn("")     // Reorder
                .thenReturn("4").thenReturn("today").thenReturn("")     // Stock
                .thenReturn("5").thenReturn("today").thenReturn("")     // Bill
                .thenReturn("6");                                        // Exit

        // Mock all reports
        LocalDate today = LocalDate.now();
        PhysicalStoreSales physicalSales = new PhysicalStoreSales(Collections.emptyList(), BigDecimal.ZERO);
        OnlineStoreSales onlineSales = new OnlineStoreSales(Collections.emptyList(), BigDecimal.ZERO);

        when(reportService.generateDailySalesReport(any(LocalDate.class)))
                .thenReturn(new DailySalesReport(today, physicalSales, onlineSales, BigDecimal.ZERO));
        when(reportService.generateRestockReport(any(LocalDate.class)))
                .thenReturn(new RestockReport(today, Collections.emptyList(), Collections.emptyList()));
        when(reportService.generateReorderReport(any(LocalDate.class)))
                .thenReturn(new ReorderReport(today, Collections.emptyList()));
        when(reportService.generateStockReport(any(LocalDate.class)))
                .thenReturn(new StockReport(today, Collections.emptyList()));
        when(reportService.generateBillReport(any(LocalDate.class)))
                .thenReturn(new BillReport(today, Collections.emptyList(), Collections.emptyList(), 0, 0));

        // Act
        controller.startManagerMode();

        // Assert
        verify(reportService).generateDailySalesReport(any(LocalDate.class));
        verify(reportService).generateRestockReport(any(LocalDate.class));
        verify(reportService).generateReorderReport(any(LocalDate.class));
        verify(reportService).generateStockReport(any(LocalDate.class));
        verify(reportService).generateBillReport(any(LocalDate.class));
        verify(ui, times(5)).waitForEnter(); // Called five times
    }
}