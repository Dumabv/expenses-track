package com.expensestrack.service;

import com.expensestrack.model.Expense;
import com.expensestrack.model.ExpenseSummary;
import com.expensestrack.model.Income;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExpenseServiceTest {

    @Test
    void notifiesListenersAndUpdatesSummaryAcrossCrudOperations() {
        ExpenseService service = new ExpenseService(
                new CalculationService(),
                Income.of(new BigDecimal("800"), "$"),
                List.of()
        );
        List<ExpenseSummary> events = new ArrayList<>();
        service.addSummaryListener(events::add);

        Expense church = Expense.of(new BigDecimal("80"), "church");
        Expense savings = Expense.of(new BigDecimal("80"), "savings");
        service.addExpense(church);
        service.addExpense(savings);

        Expense updatedSavings = new Expense(savings.id(), new BigDecimal("120"), "emergency fund");
        service.updateExpense(updatedSavings);
        service.updateIncomeAmount(new BigDecimal("1000"));
        service.updateIncomeCurrency("€");
        service.removeExpense(church.id());

        ExpenseSummary latest = service.getSummary();
        assertEquals(6, events.size());
        assertEquals("€", latest.income().currency());
        assertEquals(1, latest.lines().size());
        assertEquals(0, new BigDecimal("120").compareTo(latest.totalExpenses()));
        assertEquals(0, new BigDecimal("12").compareTo(latest.totalPercentage()));
        assertEquals(0, new BigDecimal("880").compareTo(latest.rest()));
    }

    @Test
    void replaceExpensesPreservesOrderAndRecalculates() {
        ExpenseService service = new ExpenseService(
                new CalculationService(),
                Income.of(new BigDecimal("500"), "$"),
                List.of()
        );
        Expense first = Expense.of(new BigDecimal("100"), "first");
        Expense second = Expense.of(new BigDecimal("50"), "second");

        ExpenseSummary summary = service.replaceExpenses(List.of(first, second));

        assertEquals(2, summary.lines().size());
        assertEquals("first", summary.lines().get(0).expense().description());
        assertEquals("second", summary.lines().get(1).expense().description());
        assertEquals(0, new BigDecimal("350").compareTo(summary.rest()));
    }
}
