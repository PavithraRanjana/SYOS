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
 * Fixed unit tests for SyosManagerController.
 * Properly mocks both versions of getUserInput() method.
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

        // Setup common UI behavior that all tests need
        setupCommonUIMocks();
    }

    /**
     * Sets up common UI mocks that are needed across tests.
     * Uses lenient() for stubs that might not be used in all tests.
     */
    private void setupCommonUIMocks() {
        // Mock confirmAction for error handling (might not be used in all tests)
        lenient().when(ui.confirmAction("Try again?")).thenReturn(false);

        // Mock display methods (these return void, so we just need to ensure they don't throw)
        lenient().doNothing().when(ui).clearScreen();
        lenient().doNothing().when(ui).displaySuccess(anyString());
        lenient().doNothing().when(ui).displayError(anyString());
        lenient().doNothing().when(ui).waitForEnter();
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
                .thenReturn("6");                   // Exit after report

        // Mock the getUserInput with prompt for date input
        when(ui.getUserInput(contains("Enter date for sales report")))
                .thenReturn("2024-09-24");

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
                .thenReturn("6");                   // Exit after report

        when(ui.getUserInput(contains("Enter date for sales report")))
                .thenReturn("today");

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
    @DisplayName("Should handle invalid date format")
    void shouldHandleInvalidDateFormat() {
        // Arrange
        when(ui.getUserInput())
                .thenReturn("1")                    // Select daily sales report
                .thenReturn("6");                   // Exit after error handling

        when(ui.getUserInput(contains("Enter date for sales report")))
                .thenReturn("invalid-date")        // Invalid date format
                .thenReturn("today");               // Valid fallback after error

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
    @DisplayName("Should handle null input gracefully")
    void shouldHandleNullInputGracefully() {
        // Arrange
        when(ui.getUserInput())
                .thenReturn("1")                    // Select daily sales report
                .thenReturn("6");                   // Exit

        when(ui.getUserInput(contains("Enter date for sales report")))
                .thenReturn(null)                  // Null input
                .thenReturn("today");               // Valid fallback after error

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


    // ==================== RESTOCK REPORT TESTS ====================

    @Test
    @DisplayName("Should generate restock report successfully")
    void shouldGenerateRestockReportSuccessfully() {
        // Arrange
        when(ui.getUserInput())
                .thenReturn("2")                    // Select restock report
                .thenReturn("6");                   // Exit

        when(ui.getUserInput(contains("Enter date to check inventory levels")))
                .thenReturn("2024-09-24");

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

    // ==================== REORDER REPORT TESTS ====================

    @Test
    @DisplayName("Should generate reorder report successfully")
    void shouldGenerateReorderReportSuccessfully() {
        // Arrange
        when(ui.getUserInput())
                .thenReturn("3")                    // Select reorder report
                .thenReturn("6");                   // Exit

        when(ui.getUserInput(contains("Enter date to check stock levels")))
                .thenReturn("2024-09-24");

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

    // ==================== STOCK REPORT TESTS ====================

    @Test
    @DisplayName("Should generate stock report successfully")
    void shouldGenerateStockReportSuccessfully() {
        // Arrange
        when(ui.getUserInput())
                .thenReturn("4")                    // Select stock report
                .thenReturn("6");                   // Exit

        when(ui.getUserInput(contains("Enter date to get stock details up to")))
                .thenReturn("2024-09-24");

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

    // ==================== BILL REPORT TESTS ====================

    @Test
    @DisplayName("Should generate bill report successfully")
    void shouldGenerateBillReportSuccessfully() {
        // Arrange
        when(ui.getUserInput())
                .thenReturn("5")                    // Select bill report
                .thenReturn("6");                   // Exit

        when(ui.getUserInput(contains("Enter date to get bill transactions")))
                .thenReturn("2024-09-24");

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

    // ==================== INTEGRATION TESTS ====================

    @Test
    @DisplayName("Should handle multiple report generations in sequence")
    void shouldHandleMultipleReportGenerationsInSequence() {
        // Arrange
        when(ui.getUserInput())
                .thenReturn("1")                    // Daily sales report
                .thenReturn("2")                    // Restock report
                .thenReturn("3")                    // Reorder report
                .thenReturn("6");                   // Exit

        // Mock date inputs for each report type
        when(ui.getUserInput(contains("Enter date for sales report")))
                .thenReturn("2024-09-24");
        when(ui.getUserInput(contains("Enter date to check inventory levels")))
                .thenReturn("2024-09-24");
        when(ui.getUserInput(contains("Enter date to check stock levels")))
                .thenReturn("2024-09-24");

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
    @DisplayName("Should handle service exceptions gracefully")
    void shouldHandleServiceExceptionsGracefully() {
        // Arrange
        when(ui.getUserInput())
                .thenReturn("1")                    // Select daily sales report
                .thenReturn("6");                   // Exit

        when(ui.getUserInput(contains("Enter date for sales report")))
                .thenReturn("2024-09-24");

        when(reportService.generateDailySalesReport(testDate))
                .thenThrow(new ReportServiceImpl.ReportGenerationException("Database error"));

        // Act
        controller.startManagerMode();

        // Assert
        verify(ui).displayError(contains("Failed to generate sales report"));
        verify(ui).waitForEnter();
    }
}