package com.expensestrack.service;

import com.expensestrack.model.Expense;
import com.expensestrack.model.ExpenseSummary;
import com.expensestrack.model.Income;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalculationServiceTest {

    private CalculationService calculationService;

    @BeforeEach
    void setUp() {
        calculationService = new CalculationService();
    }

    @Test
    void calculatesIndividualAndTotalSummaryForPromptExample() {
        Income income = Income.of(new BigDecimal("800"), "$");
        Expense church = Expense.of(new BigDecimal("80"), "church");
        Expense savings = Expense.of(new BigDecimal("80"), "savings");
        Expense hangout = Expense.of(new BigDecimal("125"), "hang out with friends");

        ExpenseSummary summary = calculationService.summarize(income, List.of(church, savings, hangout));

        assertEquals(0, new BigDecimal("10").compareTo(summary.percentageFor(church.id()).orElseThrow()));
        assertEquals(0, new BigDecimal("10").compareTo(summary.percentageFor(savings.id()).orElseThrow()));
        assertEquals(0, new BigDecimal("15.6").compareTo(summary.percentageFor(hangout.id()).orElseThrow()));

        assertEquals(0, new BigDecimal("285").compareTo(summary.totalExpenses()));
        assertEquals(0, new BigDecimal("35.6").compareTo(summary.totalPercentage()));
        assertEquals(0, new BigDecimal("515").compareTo(summary.rest()));
        assertEquals(0, new BigDecimal("64.4").compareTo(summary.restPercentage()));
    }

    @Test
    void omitsTrailingZeroForWholePercentages() {
        BigDecimal exactWhole = calculationService.calculatePercentage(new BigDecimal("80"), new BigDecimal("800"));
        BigDecimal roundedToWhole = calculationService.calculatePercentage(new BigDecimal("79.99"), new BigDecimal("800"));

        assertEquals("10", exactWhole.toPlainString());
        assertEquals("10", roundedToWhole.toPlainString());
    }

    @Test
    void roundsPercentagesUsingHalfUpToOneDecimalPlace() {
        // 125 / 800 * 100 = 15.625 -> 15.6
        assertEquals("15.6", calculationService.calculatePercentage(
                new BigDecimal("125"), new BigDecimal("800")).toPlainString());

        // 1 / 2000 * 100 = 0.05 -> HALF_UP -> 0.1
        assertEquals("0.1", calculationService.calculatePercentage(
                new BigDecimal("1"), new BigDecimal("2000")).toPlainString());

        // 1 / 3 * 100 = 33.333... -> 33.3
        assertEquals("33.3", calculationService.calculatePercentage(
                new BigDecimal("1"), new BigDecimal("3")).toPlainString());
    }

    @Test
    void calculatesTotalPercentageFromUnroundedAmountsInsteadOfRoundedPercentages() {
        Income income = Income.of(new BigDecimal("800"), "$");
        // Each 125 is 15.625% (rounds to 15.6%), yet 250 / 800 * 100 = 31.25% -> 31.3%
        Expense first = Expense.of(new BigDecimal("125"), "first");
        Expense second = Expense.of(new BigDecimal("125"), "second");

        ExpenseSummary summary = calculationService.summarize(income, List.of(first, second));

        assertEquals("15.6", summary.percentageFor(first.id()).orElseThrow().toPlainString());
        assertEquals("31.3", summary.totalPercentage().toPlainString());
        assertEquals("68.8", summary.restPercentage().toPlainString());
    }

    @Test
    void handlesDecimalMonetaryAmountsWithoutFloatingPointPrecisionLoss() {
        Income income = Income.of(new BigDecimal("100.00"), "€");
        Expense coffee = Expense.of(new BigDecimal("0.10"), "coffee");
        Expense snack = Expense.of(new BigDecimal("0.20"), "snack");
        Expense transport = Expense.of(new BigDecimal("33.335"), "");

        ExpenseSummary summary = calculationService.summarize(income, List.of(coffee, snack, transport));

        // Exact unrounded monetary totals: 0.10 + 0.20 + 33.335 = 33.635
        assertEquals("33.635", summary.totalExpenses().stripTrailingZeros().toPlainString());
        assertEquals("66.365", summary.rest().stripTrailingZeros().toPlainString());
    }

    @Test
    void handlesZeroIncomeSafelyWithoutArithmeticException() {
        Income zeroIncome = Income.empty();
        Expense expense = Expense.of(new BigDecimal("50"), "groceries");

        ExpenseSummary summary = calculationService.summarize(zeroIncome, List.of(expense));

        assertEquals(0, BigDecimal.ZERO.compareTo(summary.percentageFor(expense.id()).orElseThrow()));
        assertEquals(0, new BigDecimal("50").compareTo(summary.totalExpenses()));
        assertEquals(0, BigDecimal.ZERO.compareTo(summary.totalPercentage()));
        assertEquals(0, new BigDecimal("-50").compareTo(summary.rest()));
        assertEquals(0, BigDecimal.ZERO.compareTo(summary.restPercentage()));
    }

    @Test
    void handlesNegativeRemainingBalanceWhenExpensesExceedIncome() {
        Income income = Income.of(new BigDecimal("800"), "$");
        Expense rent = Expense.of(new BigDecimal("835"), "rent");

        ExpenseSummary summary = calculationService.summarize(income, List.of(rent));

        assertEquals(0, new BigDecimal("835").compareTo(summary.totalExpenses()));
        assertEquals("104.4", summary.totalPercentage().toPlainString());
        assertEquals(0, new BigDecimal("-35").compareTo(summary.rest()));
        assertEquals("-4.4", summary.restPercentage().toPlainString());
    }

    @Test
    void handlesExpensesWithEmptyDescriptions() {
        Income income = Income.of(new BigDecimal("500"), "$");
        Expense blankDescription = Expense.of(new BigDecimal("50"), "");
        Expense nullDescription = Expense.of(new BigDecimal("100"), null);

        ExpenseSummary summary = calculationService.summarize(income, List.of(blankDescription, nullDescription));

        assertEquals("", blankDescription.description());
        assertEquals("", nullDescription.description());
        assertEquals(0, new BigDecimal("150").compareTo(summary.totalExpenses()));
        assertEquals("30", summary.totalPercentage().toPlainString());
        assertEquals(0, new BigDecimal("350").compareTo(summary.rest()));
    }

    @Test
    void rejectsNegativeIncomeAndNegativeExpense() {
        assertThrows(IllegalArgumentException.class,
                () -> Income.of(new BigDecimal("-1"), "$"));
        assertThrows(IllegalArgumentException.class,
                () -> Expense.of(new BigDecimal("-0.01"), "invalid"));
        assertThrows(IllegalArgumentException.class,
                () -> calculationService.calculatePercentage(new BigDecimal("10"), new BigDecimal("-100")));
    }
}
