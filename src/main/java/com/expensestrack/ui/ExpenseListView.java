package com.expensestrack.ui;

import com.expensestrack.model.Expense;
import com.expensestrack.model.ExpenseSummary;
import javafx.application.Platform;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public class ExpenseListView extends VBox implements ExpenseRow.RowActions {

    private final List<ExpenseRow> rows = new ArrayList<>();
    private final Consumer<List<Expense>> onExpensesChanged;
    private final Runnable onNavigateAboveFirst;
    private final Runnable onScrollToBottom;

    private boolean suppressNotifications;

    public ExpenseListView(
            Consumer<List<Expense>> onExpensesChanged,
            Runnable onNavigateAboveFirst,
            Runnable onScrollToBottom
    ) {
        this.onExpensesChanged = Objects.requireNonNull(onExpensesChanged, "onExpensesChanged must not be null");
        this.onNavigateAboveFirst = Objects.requireNonNull(onNavigateAboveFirst, "onNavigateAboveFirst must not be null");
        this.onScrollToBottom = Objects.requireNonNull(onScrollToBottom, "onScrollToBottom must not be null");

        setSpacing(4);
    }

    public void setExpenses(List<Expense> loadedExpenses, ExpenseSummary initialSummary) {
        Objects.requireNonNull(loadedExpenses, "Loaded expenses must not be null");
        Objects.requireNonNull(initialSummary, "Initial summary must not be null");

        suppressNotifications = true;
        try {
            rows.clear();
            getChildren().clear();

            for (Expense expense : loadedExpenses) {
                ExpenseRow row = new ExpenseRow(expense, this);
                row.updateSummary(initialSummary);
                rows.add(row);
                getChildren().add(row);
            }

            // Always provide an empty trailing line ready for immediate typing.
            ExpenseRow trailingBlank = new ExpenseRow(this);
            rows.add(trailingBlank);
            getChildren().add(trailingBlank);
        } finally {
            suppressNotifications = false;
        }
    }

    public void updateSummary(ExpenseSummary summary) {
        Objects.requireNonNull(summary, "Summary must not be null");
        for (ExpenseRow row : rows) {
            row.updateSummary(summary);
        }
    }

    public void focusFirstRow() {
        if (!rows.isEmpty()) {
            rows.get(0).focusAmountAtEnd();
        }
    }

    public void focusLastRow() {
        if (!rows.isEmpty()) {
            rows.get(rows.size() - 1).focusAmountAtEnd();
        }
    }

    @Override
    public void onRowContentChanged() {
        if (suppressNotifications) {
            return;
        }
        List<Expense> validExpenses = rows.stream()
                .map(ExpenseRow::toExpense)
                .flatMap(Optional::stream)
                .toList();
        onExpensesChanged.accept(validExpenses);
    }

    @Override
    public void onCreateRowAfter(ExpenseRow anchor) {
        int index = rows.indexOf(anchor);
        int insertIndex = index >= 0 ? index + 1 : rows.size();

        ExpenseRow newRow = new ExpenseRow(this);
        rows.add(insertIndex, newRow);
        getChildren().add(insertIndex, newRow);

        Platform.runLater(() -> {
            newRow.focusAmountAtEnd();
            if (insertIndex == rows.size() - 1) {
                onScrollToBottom.run();
            }
        });
    }

    @Override
    public void onDeleteRow(ExpenseRow row, boolean fromEmptyBackspace) {
        int index = rows.indexOf(row);
        if (index < 0) {
            return;
        }

        if (rows.size() == 1) {
            row.clearInputs();
            Platform.runLater(row::focusAmountAtEnd);
            return;
        }

        rows.remove(index);
        getChildren().remove(row);
        onRowContentChanged();

        int targetIndex = Math.max(0, index - 1);
        ExpenseRow targetRow = rows.get(targetIndex);
        Platform.runLater(() -> {
            if (fromEmptyBackspace && index > 0) {
                targetRow.focusDescriptionAtEnd();
            } else {
                targetRow.focusAmountAtEnd();
            }
        });
    }

    @Override
    public void onNavigateUp(ExpenseRow row, boolean fromAmountField) {
        int index = rows.indexOf(row);
        if (index <= 0) {
            onNavigateAboveFirst.run();
            return;
        }
        ExpenseRow previous = rows.get(index - 1);
        if (fromAmountField) {
            previous.focusAmountAtEnd();
        } else {
            previous.focusDescriptionAtEnd();
        }
    }

    @Override
    public void onNavigateDown(ExpenseRow row, boolean fromAmountField) {
        int index = rows.indexOf(row);
        if (index < 0 || index >= rows.size() - 1) {
            return;
        }
        ExpenseRow next = rows.get(index + 1);
        if (fromAmountField) {
            next.focusAmountAtEnd();
        } else {
            next.focusDescriptionAtEnd();
        }
    }
}
