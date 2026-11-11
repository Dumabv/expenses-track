package com.expensestrack.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record ExpenseSummary(
        Income income,
        List<Line> lines,
        BigDecimal totalExpenses,
        BigDecimal totalPercentage,
        BigDecimal rest,
        BigDecimal restPercentage
) {

    public ExpenseSummary {
        Objects.requireNonNull(income, "Income must not be null");
        Objects.requireNonNull(lines, "Lines must not be null");
        Objects.requireNonNull(totalExpenses, "Total expenses must not be null");
        Objects.requireNonNull(totalPercentage, "Total percentage must not be null");
        Objects.requireNonNull(rest, "Rest must not be null");
        Objects.requireNonNull(restPercentage, "Rest percentage must not be null");
        lines = List.copyOf(lines);
    }

    public Optional<BigDecimal> percentageFor(String expenseId) {
        if (expenseId == null) {
            return Optional.empty();
        }
        return lines.stream()
                .filter(line -> line.expense().id().equals(expenseId))
                .map(Line::percentage)
                .findFirst();
    }

    public record Line(Expense expense, BigDecimal percentage) {
        public Line {
            Objects.requireNonNull(expense, "Expense must not be null");
            Objects.requireNonNull(percentage, "Percentage must not be null");
        }
    }
}
