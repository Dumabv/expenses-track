package com.expensestrack.storage;

import com.expensestrack.model.Expense;
import com.expensestrack.model.ExpenseSummary;
import com.expensestrack.model.Income;

import java.util.List;
import java.util.Objects;

public record TrackerData(Income income, List<Expense> expenses) {

    public TrackerData {
        Objects.requireNonNull(income, "Income must not be null");
        Objects.requireNonNull(expenses, "Expenses list must not be null");
        expenses = List.copyOf(expenses);
    }

    public static TrackerData empty() {
        return new TrackerData(Income.empty(), List.of());
    }

    public static TrackerData fromSummary(ExpenseSummary summary) {
        Objects.requireNonNull(summary, "Summary must not be null");
        List<Expense> expenses = summary.lines().stream()
                .map(ExpenseSummary.Line::expense)
                .toList();
        return new TrackerData(summary.income(), expenses);
    }
}
