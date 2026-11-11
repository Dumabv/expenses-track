package com.expensestrack.service;

import com.expensestrack.model.Expense;
import com.expensestrack.model.ExpenseSummary;
import com.expensestrack.model.Income;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class ExpenseService {

    private final CalculationService calculationService;
    private final List<Expense> expenses = new ArrayList<>();
    private final List<Consumer<ExpenseSummary>> listeners = new CopyOnWriteArrayList<>();

    private Income income;
    private ExpenseSummary currentSummary;

    public ExpenseService(CalculationService calculationService) {
        this(calculationService, Income.empty(), List.of());
    }

    public ExpenseService(CalculationService calculationService, Income initialIncome, List<Expense> initialExpenses) {
        this.calculationService = Objects.requireNonNull(calculationService, "CalculationService must not be null");
        this.income = Objects.requireNonNull(initialIncome, "Initial income must not be null");
        Objects.requireNonNull(initialExpenses, "Initial expenses must not be null");
        this.expenses.addAll(initialExpenses);
        this.currentSummary = calculationService.summarize(this.income, this.expenses);
    }

    public synchronized Income getIncome() {
        return income;
    }

    public synchronized List<Expense> getExpenses() {
        return List.copyOf(expenses);
    }

    public synchronized ExpenseSummary getSummary() {
        return currentSummary;
    }

    public ExpenseSummary updateIncomeAmount(BigDecimal amount) {
        Objects.requireNonNull(amount, "Income amount must not be null");
        ExpenseSummary updated;
        synchronized (this) {
            income = income.withAmount(amount);
            updated = recalculate();
        }
        notifyListeners(updated);
        return updated;
    }

    public ExpenseSummary updateIncomeCurrency(String currency) {
        ExpenseSummary updated;
        synchronized (this) {
            income = income.withCurrency(currency);
            updated = recalculate();
        }
        notifyListeners(updated);
        return updated;
    }

    public ExpenseSummary addExpense(Expense expense) {
        Objects.requireNonNull(expense, "Expense must not be null");
        ExpenseSummary updated;
        synchronized (this) {
            expenses.add(expense);
            updated = recalculate();
        }
        notifyListeners(updated);
        return updated;
    }

    public ExpenseSummary updateExpense(Expense updatedExpense) {
        Objects.requireNonNull(updatedExpense, "Updated expense must not be null");
        ExpenseSummary updated;
        synchronized (this) {
            int index = findExpenseIndex(updatedExpense.id());
            if (index >= 0) {
                expenses.set(index, updatedExpense);
            } else {
                expenses.add(updatedExpense);
            }
            updated = recalculate();
        }
        notifyListeners(updated);
        return updated;
    }

    public ExpenseSummary removeExpense(String expenseId) {
        Objects.requireNonNull(expenseId, "Expense id must not be null");
        ExpenseSummary updated;
        synchronized (this) {
            int index = findExpenseIndex(expenseId);
            if (index < 0) {
                return currentSummary;
            }
            expenses.remove(index);
            updated = recalculate();
        }
        notifyListeners(updated);
        return updated;
    }

    public ExpenseSummary replaceExpenses(List<Expense> updatedExpenses) {
        Objects.requireNonNull(updatedExpenses, "Expenses list must not be null");
        List<Expense> copy = List.copyOf(updatedExpenses);
        ExpenseSummary updated;
        synchronized (this) {
            expenses.clear();
            expenses.addAll(copy);
            updated = recalculate();
        }
        notifyListeners(updated);
        return updated;
    }

    public void addSummaryListener(Consumer<ExpenseSummary> listener) {
        Objects.requireNonNull(listener, "Listener must not be null");
        listeners.add(listener);
    }

    private int findExpenseIndex(String expenseId) {
        for (int i = 0; i < expenses.size(); i++) {
            if (expenses.get(i).id().equals(expenseId)) {
                return i;
            }
        }
        return -1;
    }

    private ExpenseSummary recalculate() {
        currentSummary = calculationService.summarize(income, expenses);
        return currentSummary;
    }

    private void notifyListeners(ExpenseSummary summary) {
        for (Consumer<ExpenseSummary> listener : listeners) {
            listener.accept(summary);
        }
    }
}
