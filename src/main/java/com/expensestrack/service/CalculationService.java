package com.expensestrack.service;

import com.expensestrack.model.Expense;
import com.expensestrack.model.ExpenseSummary;
import com.expensestrack.model.Income;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

public class CalculationService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final int PERCENTAGE_SCALE = 1;

    public BigDecimal calculatePercentage(BigDecimal amount, BigDecimal income) {
        Objects.requireNonNull(amount, "Amount must not be null");
        Objects.requireNonNull(income, "Income must not be null");
        if (income.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Income cannot be negative: " + income);
        }
        if (income.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // Multiply before dividing so HALF_UP rounding happens in a single division step.
        BigDecimal rounded = amount.multiply(ONE_HUNDRED)
                .divide(income, PERCENTAGE_SCALE, RoundingMode.HALF_UP);
        return normalizePercentage(rounded);
    }

    public ExpenseSummary summarize(Income income, List<Expense> expenses) {
        Objects.requireNonNull(income, "Income must not be null");
        Objects.requireNonNull(expenses, "Expenses list must not be null");

        BigDecimal totalExpenses = BigDecimal.ZERO;
        for (Expense expense : expenses) {
            Objects.requireNonNull(expense, "Expense item must not be null");
            totalExpenses = totalExpenses.add(expense.amount());
        }

        BigDecimal incomeAmount = income.amount();
        BigDecimal rest = incomeAmount.subtract(totalExpenses);

        List<ExpenseSummary.Line> lines = expenses.stream()
                .map(expense -> new ExpenseSummary.Line(
                        expense,
                        calculatePercentage(expense.amount(), incomeAmount)
                ))
                .toList();

        BigDecimal totalPercentage = calculatePercentage(totalExpenses, incomeAmount);
        BigDecimal restPercentage = calculatePercentage(rest, incomeAmount);

        return new ExpenseSummary(income, lines, totalExpenses, totalPercentage, rest, restPercentage);
    }

    private BigDecimal normalizePercentage(BigDecimal percentage) {
        if (percentage.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (percentage.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
            return percentage.setScale(0, RoundingMode.UNNECESSARY);
        }
        return percentage;
    }
}
