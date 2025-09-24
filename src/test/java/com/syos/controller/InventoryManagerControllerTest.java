package com.syos.controller;

import com.syos.domain.valueobjects.ProductCode;
import com.syos.domain.valueobjects.Money;
import com.syos.domain.enums.UnitOfMeasure;
import com.syos.exceptions.BusinessRuleException;
import com.syos.service.interfaces.InventoryManagerService;
import com.syos.ui.interfaces.UserInterface;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure unit tests for InventoryManagerController business logic methods.
 * Tests all validation rules, edge cases, and business logic without UI dependencies.
 * Target: 80%+ coverage focusing on business logic.
 */
@ExtendWith(MockitoExtension.class)
class InventoryManagerControllerPureLogicTest {

    @Mock
    private InventoryManagerService inventoryManagerService;

    @Mock
    private UserInterface ui;

    private InventoryManagerController controller;

    @BeforeEach
    void setUp() {
        controller = new InventoryManagerController(inventoryManagerService, ui);
    }

    // ==================== PRODUCT CODE PARSING TESTS ====================

    @Test
    @DisplayName("Should parse valid product code successfully")
    void shouldParseValidProductCodeSuccessfully() {
        // Test various valid formats
        ProductCode result1 = controller.parseProductCode("FDMCCM001");
        assertEquals("FDMCCM001", result1.getCode());

        ProductCode result2 = controller.parseProductCode("  bvedrb001  ");
        assertEquals("BVEDRB001", result2.getCode()); // Should be trimmed and uppercase

        ProductCode result3 = controller.parseProductCode("chgod001");
        assertEquals("CHGOD001", result3.getCode());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t", "\n"})
    @DisplayName("Should reject null/empty/whitespace product codes")
    void shouldRejectInvalidProductCodes(String input) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> controller.parseProductCode(input));

        if (input == null) {
            assertTrue(exception.getMessage().contains("cannot be null"));
        } else {
            assertTrue(exception.getMessage().contains("cannot be empty"));
        }
    }

    @Test
    @DisplayName("Should handle ProductCode validation errors")
    void shouldHandleProductCodeValidationErrors() {
        // This would trigger ProductCode constructor validation
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> controller.parseProductCode("INVALID_FORMAT_TOO_LONG"));

        assertTrue(exception.getMessage().contains("Invalid product code format"));
    }

    // ==================== INTEGER PARSING TESTS ====================

    @Test
    @DisplayName("Should parse valid integers successfully")
    void shouldParseValidIntegersSuccessfully() {
        assertEquals(123, controller.parseInteger("123"));
        assertEquals(-456, controller.parseInteger("-456"));
        assertEquals(0, controller.parseInteger("0"));
        assertEquals(789, controller.parseInteger("  789  ")); // Trimmed
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t"})
    @DisplayName("Should reject null/empty integer input")
    void shouldRejectNullOrEmptyIntegerInput(String input) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> controller.parseInteger(input));

        if (input == null) {
            assertTrue(exception.getMessage().contains("cannot be null"));
        } else {
            assertTrue(exception.getMessage().contains("cannot be empty"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "12.34", "1a2b", "2147483648"}) // Last one exceeds Integer.MAX_VALUE
    @DisplayName("Should reject invalid integer formats")
    void shouldRejectInvalidIntegerFormats(String input) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> controller.parseInteger(input));

        assertTrue(exception.getMessage().contains("Invalid number format"));
        assertTrue(exception.getMessage().contains(input));
    }

    // ==================== POSITIVE INTEGER TESTS ====================

    @Test
    @DisplayName("Should parse valid positive integers")
    void shouldParseValidPositiveIntegers() {
        assertEquals(1, controller.parsePositiveInteger("1"));
        assertEquals(100, controller.parsePositiveInteger("100"));
        assertEquals(999999, controller.parsePositiveInteger("999999"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "-100"})
    @DisplayName("Should reject non-positive integers")
    void shouldRejectNonPositiveIntegers(String input) {
        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> controller.parsePositiveInteger(input));

        assertTrue(exception.getMessage().contains("must be positive"));
        assertTrue(exception.getMessage().contains(input));
    }

    // ==================== BIG DECIMAL PARSING TESTS ====================

    @Test
    @DisplayName("Should parse valid positive decimal amounts")
    void shouldParseValidPositiveDecimalAmounts() {
        assertEquals(new BigDecimal("123.45"), controller.parsePositiveBigDecimal("123.45"));
        assertEquals(new BigDecimal("0.01"), controller.parsePositiveBigDecimal("0.01"));
        assertEquals(new BigDecimal("1000"), controller.parsePositiveBigDecimal("1000"));
        assertEquals(new BigDecimal("99.99"), controller.parsePositiveBigDecimal("  99.99  ")); // Trimmed
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t"})
    @DisplayName("Should reject null/empty decimal input")
    void shouldRejectNullOrEmptyDecimalInput(String input) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> controller.parsePositiveBigDecimal(input));

        if (input == null) {
            assertTrue(exception.getMessage().contains("cannot be null"));
        } else {
            assertTrue(exception.getMessage().contains("cannot be empty"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "12.34.56", "not_a_number"})
    @DisplayName("Should reject invalid decimal formats")
    void shouldRejectInvalidDecimalFormats(String input) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> controller.parsePositiveBigDecimal(input));

        assertTrue(exception.getMessage().contains("Invalid amount format"));
        assertTrue(exception.getMessage().contains(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-0.01", "-100.00"})
    @DisplayName("Should reject non-positive decimal amounts")
    void shouldRejectNonPositiveDecimalAmounts(String input) {
        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> controller.parsePositiveBigDecimal(input));

        assertTrue(exception.getMessage().contains("must be positive"));
        assertTrue(exception.getMessage().contains(input));
    }

    // ==================== MONEY CREATION TESTS ====================

    @Test
    @DisplayName("Should create Money from valid BigDecimal")
    void shouldCreateMoneyFromValidBigDecimal() {
        BigDecimal amount = new BigDecimal("150.75");
        Money result = controller.createMoney(amount);

        assertEquals(amount, result.getAmount());
    }

    @Test
    @DisplayName("Should reject null BigDecimal for Money creation")
    void shouldRejectNullBigDecimalForMoneyCreation() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> controller.createMoney(null));

        assertTrue(exception.getMessage().contains("Amount cannot be null"));
    }

    @Test
    @DisplayName("Should parse Money from string successfully")
    void shouldParseMoneyFromStringSuccessfully() {
        Money result = controller.parseMoney("199.99");
        assertEquals(new BigDecimal("199.99"), result.getAmount());
    }

    // ==================== DATE PARSING TESTS ====================

    @Test
    @DisplayName("Should parse valid business dates")
    void shouldParseValidBusinessDates() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate lastYear = today.minusYears(1);

        assertEquals(yesterday, controller.parseBusinessDate(yesterday.toString()));
        assertEquals(lastYear, controller.parseBusinessDate(lastYear.toString()));
        assertEquals(today, controller.parseBusinessDate(today.toString()));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t"})
    @DisplayName("Should reject null/empty date input")
    void shouldRejectNullOrEmptyDateInput(String input) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> controller.parseBusinessDate(input));

        if (input == null) {
            assertTrue(exception.getMessage().contains("cannot be null"));
        } else {
            assertTrue(exception.getMessage().contains("cannot be empty"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid-date", "2024/01/15", "15-01-2024", "2024-13-01", "2024-01-32"})
    @DisplayName("Should reject invalid date formats")
    void shouldRejectInvalidDateFormats(String input) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> controller.parseBusinessDate(input));

        assertTrue(exception.getMessage().contains("Invalid date format"));
    }

    @Test
    @DisplayName("Should reject future dates")
    void shouldRejectFutureDates() {
        LocalDate futureDate = LocalDate.now().plusDays(1);

        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> controller.parseBusinessDate(futureDate.toString()));

        assertTrue(exception.getMessage().contains("cannot be in the future"));
        assertTrue(exception.getMessage().contains(futureDate.toString()));
    }

    // ==================== OPTIONAL EXPIRY DATE TESTS ====================

    @Test
    @DisplayName("Should handle optional expiry dates")
    void shouldHandleOptionalExpiryDates() {
        // Valid dates
        LocalDate futureDate = LocalDate.now().plusMonths(6);
        assertEquals(futureDate, controller.parseOptionalExpiryDate(futureDate.toString()));

        // Null/empty should return null
        assertNull(controller.parseOptionalExpiryDate(null));
        assertNull(controller.parseOptionalExpiryDate(""));
        assertNull(controller.parseOptionalExpiryDate("  "));

        // Past dates are allowed for expiry (expired products)
        LocalDate pastDate = LocalDate.now().minusDays(1);
        assertEquals(pastDate, controller.parseOptionalExpiryDate(pastDate.toString()));
    }

    @Test
    @DisplayName("Should reject invalid expiry date formats")
    void shouldRejectInvalidExpiryDateFormats() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> controller.parseOptionalExpiryDate("invalid-date"));

        assertTrue(exception.getMessage().contains("Invalid expiry date format"));
    }

    // ==================== UNIT OF MEASURE CHOICE TESTS ====================

    @Test
    @DisplayName("Should parse valid unit of measure choices")
    void shouldParseValidUnitOfMeasureChoices() {
        UnitOfMeasure[] units = UnitOfMeasure.values();

        // Test all valid choices
        for (int i = 0; i < units.length; i++) {
            UnitOfMeasure result = controller.parseUnitOfMeasureChoice(i + 1);
            assertEquals(units[i], result);
        }
    }

    @Test
    @DisplayName("Should reject invalid unit of measure choices")
    void shouldRejectInvalidUnitOfMeasureChoices() {
        int maxChoice = UnitOfMeasure.values().length;

        // Test boundary violations
        BusinessRuleException exception1 = assertThrows(BusinessRuleException.class,
                () -> controller.parseUnitOfMeasureChoice(0));
        assertTrue(exception1.getMessage().contains("Invalid choice: 0"));

        BusinessRuleException exception2 = assertThrows(BusinessRuleException.class,
                () -> controller.parseUnitOfMeasureChoice(maxChoice + 1));
        assertTrue(exception2.getMessage().contains("Invalid choice: " + (maxChoice + 1)));

        BusinessRuleException exception3 = assertThrows(BusinessRuleException.class,
                () -> controller.parseUnitOfMeasureChoice(-5));
        assertTrue(exception3.getMessage().contains("Invalid choice: -5"));
    }

    // ==================== BATCH NUMBER TESTS ====================

    @Test
    @DisplayName("Should parse valid batch numbers")
    void shouldParseValidBatchNumbers() {
        assertEquals(1, controller.parseBatchNumber("1"));
        assertEquals(999, controller.parseBatchNumber("999"));
        assertEquals(123456, controller.parseBatchNumber("123456"));
    }

    @Test
    @DisplayName("Should reject invalid batch numbers")
    void shouldRejectInvalidBatchNumbers() {
        BusinessRuleException exception1 = assertThrows(BusinessRuleException.class,
                () -> controller.parseBatchNumber("0"));
        assertTrue(exception1.getMessage().contains("must be at least 1"));

        BusinessRuleException exception2 = assertThrows(BusinessRuleException.class,
                () -> controller.parseBatchNumber("-1"));
        assertTrue(exception2.getMessage().contains("must be at least 1"));
    }

    // ==================== THRESHOLD PARSING TESTS ====================

    @Test
    @DisplayName("Should parse threshold values with defaults")
    void shouldParseThresholdValuesWithDefaults() {
        int defaultValue = 50;

        // Valid positive values
        assertEquals(30, controller.parseThresholdWithDefault("30", defaultValue));
        assertEquals(100, controller.parseThresholdWithDefault("100", defaultValue));

        // Zero or negative should return default
        assertEquals(defaultValue, controller.parseThresholdWithDefault("0", defaultValue));
        assertEquals(defaultValue, controller.parseThresholdWithDefault("-10", defaultValue));

        // Invalid input should return default
        assertEquals(defaultValue, controller.parseThresholdWithDefault("invalid", defaultValue));
        assertEquals(defaultValue, controller.parseThresholdWithDefault("", defaultValue));
        assertEquals(defaultValue, controller.parseThresholdWithDefault(null, defaultValue));
    }

    // ==================== SUPPLIER NAME TESTS ====================

    @Test
    @DisplayName("Should parse optional supplier names")
    void shouldParseOptionalSupplierNames() {
        // Valid names
        assertEquals("ACME Corp", controller.parseOptionalSupplierName("ACME Corp"));
        assertEquals("Supplier Inc", controller.parseOptionalSupplierName("  Supplier Inc  "));

        // Null/empty should return null
        assertNull(controller.parseOptionalSupplierName(null));
        assertNull(controller.parseOptionalSupplierName(""));
        assertNull(controller.parseOptionalSupplierName("  "));
    }

    @Test
    @DisplayName("Should reject supplier names that are too long")
    void shouldRejectSupplierNamesThatAreTooLong() {
        String longName = "A".repeat(101); // 101 characters

        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> controller.parseOptionalSupplierName(longName));

        assertTrue(exception.getMessage().contains("Supplier name too long"));
        assertTrue(exception.getMessage().contains("Maximum 100 characters"));
    }

    @Test
    @DisplayName("Should accept supplier names at maximum length")
    void shouldAcceptSupplierNamesAtMaximumLength() {
        String maxLengthName = "A".repeat(100); // Exactly 100 characters
        assertEquals(maxLengthName, controller.parseOptionalSupplierName(maxLengthName));
    }

    // ==================== PRODUCT NAME TESTS ====================

    @Test
    @DisplayName("Should parse valid product names")
    void shouldParseValidProductNames() {
        assertEquals("Red Bull Energy Drink", controller.parseProductName("Red Bull Energy Drink"));
        assertEquals("Coca-Cola", controller.parseProductName("  Coca-Cola  ")); // Trimmed
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t"})
    @DisplayName("Should reject null/empty product names")
    void shouldRejectNullOrEmptyProductNames(String input) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> controller.parseProductName(input));

        if (input == null) {
            assertTrue(exception.getMessage().contains("cannot be null"));
        } else {
            assertTrue(exception.getMessage().contains("cannot be empty"));
        }
    }

    @Test
    @DisplayName("Should reject product names that are too long")
    void shouldRejectProductNamesThatAreTooLong() {
        String longName = "A".repeat(256); // 256 characters

        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> controller.parseProductName(longName));

        assertTrue(exception.getMessage().contains("Product name too long"));
        assertTrue(exception.getMessage().contains("Maximum 255 characters"));
    }

    // ==================== DESCRIPTION TESTS ====================

    @Test
    @DisplayName("Should parse optional descriptions")
    void shouldParseOptionalDescriptions() {
        assertEquals("Great product", controller.parseOptionalDescription("Great product"));
        assertEquals("Trimmed desc", controller.parseOptionalDescription("  Trimmed desc  "));

        // Null/empty should return null
        assertNull(controller.parseOptionalDescription(null));
        assertNull(controller.parseOptionalDescription(""));
        assertNull(controller.parseOptionalDescription("  "));
    }

    @Test
    @DisplayName("Should reject descriptions that are too long")
    void shouldRejectDescriptionsThatAreTooLong() {
        String longDescription = "A".repeat(1001); // 1001 characters

        BusinessRuleException exception = assertThrows(BusinessRuleException.class,
                () -> controller.parseOptionalDescription(longDescription));

        assertTrue(exception.getMessage().contains("Description too long"));
        assertTrue(exception.getMessage().contains("Maximum 1000 characters"));
    }

    // ==================== MENU VALIDATION TESTS ====================

    @Test
    @DisplayName("Should validate menu options correctly")
    void shouldValidateMenuOptionsCorrectly() {
        // Valid options
        assertTrue(controller.isValidMenuOption("1", 1, 10));
        assertTrue(controller.isValidMenuOption("5", 1, 10));
        assertTrue(controller.isValidMenuOption("10", 1, 10));

        // Invalid options
        assertFalse(controller.isValidMenuOption("0", 1, 10)); // Below range
        assertFalse(controller.isValidMenuOption("11", 1, 10)); // Above range
        assertFalse(controller.isValidMenuOption("abc", 1, 10)); // Invalid format
        assertFalse(controller.isValidMenuOption("", 1, 10)); // Empty
        assertFalse(controller.isValidMenuOption(null, 1, 10)); // Null
        assertFalse(controller.isValidMenuOption("  ", 1, 10)); // Whitespace
    }

    // ==================== BATCH COST CALCULATION TESTS ====================

    @Test
    @DisplayName("Should calculate batch total cost correctly")
    void shouldCalculateBatchTotalCostCorrectly() {
        Money unitPrice = new Money(new BigDecimal("10.50"));
        Money result = controller.calculateBatchTotalCost(unitPrice, 5);

        assertEquals(new Money(new BigDecimal("52.50")), result);
    }

    @Test
    @DisplayName("Should handle edge cases in cost calculation")
    void shouldHandleEdgeCasesInCostCalculation() {
        Money unitPrice = new Money(new BigDecimal("0.01"));
        Money result = controller.calculateBatchTotalCost(unitPrice, 1);

        assertEquals(new Money(new BigDecimal("0.01")), result);
    }

    @Test
    @DisplayName("Should reject null unit price for cost calculation")
    void shouldRejectNullUnitPriceForCostCalculation() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> controller.calculateBatchTotalCost(null, 5));

        assertTrue(exception.getMessage().contains("Unit price cannot be null"));
    }

    @Test
    @DisplayName("Should reject non-positive quantity for cost calculation")
    void shouldRejectNonPositiveQuantityForCostCalculation() {
        Money unitPrice = new Money(new BigDecimal("10.00"));

        IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class,
                () -> controller.calculateBatchTotalCost(unitPrice, 0));
        assertTrue(exception1.getMessage().contains("Quantity must be positive"));

        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class,
                () -> controller.calculateBatchTotalCost(unitPrice, -5));
        assertTrue(exception2.getMessage().contains("Quantity must be positive"));
    }

    // ==================== COMPREHENSIVE INTEGRATION TESTS ====================

    @Test
    @DisplayName("Should handle complete product creation data flow")
    void shouldHandleCompleteProductCreationDataFlow() {
        // Test complete flow with all validations
        String productName = controller.parseProductName("Red Bull Energy Drink");
        Money unitPrice = controller.parseMoney("250.50");
        String description = controller.parseOptionalDescription("Premium energy drink");
        UnitOfMeasure unit = controller.parseUnitOfMeasureChoice(1); // First enum value

        assertNotNull(productName);
        assertNotNull(unitPrice);
        assertNotNull(description);
        assertNotNull(unit);

        assertEquals("Red Bull Energy Drink", productName);
        assertEquals(new BigDecimal("250.50"), unitPrice.getAmount());
        assertEquals("Premium energy drink", description);
        assertEquals(UnitOfMeasure.PCS, unit);
    }

    @Test
    @DisplayName("Should handle complete batch creation data flow")
    void shouldHandleCompleteBatchCreationDataFlow() {
        // Test complete batch creation validation chain
        ProductCode productCode = controller.parseProductCode("BVEDRB001");
        int quantity = controller.parsePositiveInteger("100");
        Money purchasePrice = controller.parseMoney("200.00");
        LocalDate purchaseDate = controller.parseBusinessDate("2024-01-15");
        LocalDate expiryDate = controller.parseOptionalExpiryDate("2024-07-15");
        String supplier = controller.parseOptionalSupplierName("Monster Corp");
        Money totalCost = controller.calculateBatchTotalCost(purchasePrice, quantity);

        assertAll(
                () -> assertEquals("BVEDRB001", productCode.getCode()),
                () -> assertEquals(100, quantity),
                () -> assertEquals(new BigDecimal("200.00"), purchasePrice.getAmount()),
                () -> assertEquals(LocalDate.of(2024, 1, 15), purchaseDate),
                () -> assertEquals(LocalDate.of(2024, 7, 15), expiryDate),
                () -> assertEquals("Monster Corp", supplier),
                () -> assertEquals(new Money(new BigDecimal("20000.00")), totalCost)
        );
    }
}