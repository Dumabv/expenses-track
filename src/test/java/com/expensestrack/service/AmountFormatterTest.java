package com.expensestrack.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmountFormatterTest {

    @Test
    void parsesValidNonNegativeNumbersAndDecimalCommas() {
        assertEquals(Optional.of(new BigDecimal("800")), AmountFormatter.parseNonNegativeAmount("800"));
        assertEquals(Optional.of(new BigDecimal("12.50")), AmountFormatter.parseNonNegativeAmount(" 12.50 "));
        assertEquals(Optional.of(new BigDecimal("12.5")), AmountFormatter.parseNonNegativeAmount("12,5"));
        assertEquals(Optional.of(new BigDecimal("0.75")), AmountFormatter.parseNonNegativeAmount(".75"));
    }

    @Test
    void rejectsNegativeBlankAndMalformedNumericInputs() {
        assertTrue(AmountFormatter.parseNonNegativeAmount(null).isEmpty());
        assertTrue(AmountFormatter.parseNonNegativeAmount("   ").isEmpty());
        assertTrue(AmountFormatter.parseNonNegativeAmount(".").isEmpty());
        assertTrue(AmountFormatter.parseNonNegativeAmount("-50").isEmpty());
        assertTrue(AmountFormatter.parseNonNegativeAmount("1e5").isEmpty());
        assertTrue(AmountFormatter.parseNonNegativeAmount("12.3.4").isEmpty());
        assertTrue(AmountFormatter.parseNonNegativeAmount("abc").isEmpty());

        assertFalse(AmountFormatter.isValidEditableInput("-50"));
        assertFalse(AmountFormatter.isValidEditableInput("abc"));
        assertTrue(AmountFormatter.isValidEditableInput(""));
        assertTrue(AmountFormatter.isValidEditableInput("."));
    }

    @Test
    void formatsMoneyAndPercentagesCleanly() {
        assertEquals("285$", AmountFormatter.formatMoney(new BigDecimal("285.00"), "$"));
        assertEquals("12.5$", AmountFormatter.formatMoney(new BigDecimal("12.50"), "$"));
        assertEquals("-35$", AmountFormatter.formatMoney(new BigDecimal("-35"), "$"));
        assertEquals("515 MDL", AmountFormatter.formatMoney(new BigDecimal("515"), "MDL"));

        assertEquals("10%", AmountFormatter.formatPercentage(new BigDecimal("10.0")));
        assertEquals("15.6%", AmountFormatter.formatPercentage(new BigDecimal("15.6")));
        assertEquals("0%", AmountFormatter.formatPercentage(BigDecimal.ZERO));
        assertEquals("// - 10%", AmountFormatter.formatExpenseComment(new BigDecimal("10")));
    }
}
