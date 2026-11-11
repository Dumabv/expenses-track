package com.expensestrack.ui;

import com.expensestrack.model.Expense;
import com.expensestrack.model.ExpenseSummary;
import com.expensestrack.service.AmountFormatter;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class ExpenseRow extends HBox {

    public interface RowActions {
        void onRowContentChanged();

        void onCreateRowAfter(ExpenseRow row);

        void onDeleteRow(ExpenseRow row, boolean fromEmptyBackspace);

        void onNavigateUp(ExpenseRow row, boolean fromAmountField);

        void onNavigateDown(ExpenseRow row, boolean fromAmountField);
    }

    private final String expenseId;
    private final TextField amountField;
    private final TextField descriptionField;
    private final Label percentageLabel;
    private final Button deleteButton;
    private final RowActions actions;

    private boolean suppressEvents;

    public ExpenseRow(RowActions actions) {
        this(UUID.randomUUID().toString(), "", "", actions);
    }

    public ExpenseRow(Expense expense, RowActions actions) {
        this(
                expense.id(),
                AmountFormatter.formatAmount(expense.amount()),
                expense.description(),
                actions
        );
    }

    private ExpenseRow(String expenseId, String initialAmount, String initialDescription, RowActions actions) {
        this.expenseId = Objects.requireNonNull(expenseId, "Expense id must not be null");
        this.actions = Objects.requireNonNull(actions, "RowActions must not be null");

        setAlignment(Pos.CENTER_LEFT);
        setSpacing(4);
        getStyleClass().add("expense-row");

        amountField = new TextField(initialAmount);
        amountField.setPromptText("0");
        amountField.getStyleClass().addAll("editor-field", "amount-field");
        UiFormatting.applyNumericFilter(amountField);
        UiFormatting.makeAutoSizing(amountField, 24, 160);

        descriptionField = new TextField(initialDescription);
        descriptionField.setPromptText("description");
        descriptionField.getStyleClass().addAll("editor-field", "description-field");
        HBox.setHgrow(descriptionField, Priority.ALWAYS);

        percentageLabel = new Label();
        percentageLabel.getStyleClass().add("percentage-comment");
        percentageLabel.setAlignment(Pos.CENTER_LEFT);
        percentageLabel.setMinWidth(98);
        percentageLabel.setPrefWidth(Region.USE_COMPUTED_SIZE);

        deleteButton = new Button("×");
        deleteButton.getStyleClass().add("row-delete-btn");
        deleteButton.setFocusTraversable(false);
        deleteButton.setMinWidth(Region.USE_PREF_SIZE);
        deleteButton.setTooltip(new Tooltip("Remove expense (Ctrl/Cmd+Backspace)"));
        deleteButton.opacityProperty().bind(
                Bindings.when(hoverProperty()
                                .or(amountField.focusedProperty())
                                .or(descriptionField.focusedProperty()))
                        .then(0.75)
                        .otherwise(0.0)
        );
        deleteButton.setOnAction(event -> actions.onDeleteRow(this, false));

        getChildren().addAll(amountField, descriptionField, percentageLabel, deleteButton);

        wireListeners();
    }

    public String getExpenseId() {
        return expenseId;
    }

    public Optional<Expense> toExpense() {
        return AmountFormatter.parseNonNegativeAmount(amountField.getText())
                .map(amount -> new Expense(expenseId, amount, descriptionField.getText()));
    }

    public void updateSummary(ExpenseSummary summary) {
        Objects.requireNonNull(summary, "Summary must not be null");
        Optional<BigDecimal> percentage = summary.percentageFor(expenseId);
        percentageLabel.setText(percentage.map(AmountFormatter::formatExpenseComment).orElse(""));
    }

    public void clearInputs() {
        suppressEvents = true;
        try {
            amountField.clear();
            descriptionField.clear();
            percentageLabel.setText("");
        } finally {
            suppressEvents = false;
        }
        actions.onRowContentChanged();
    }

    public void focusAmountAtEnd() {
        amountField.requestFocus();
        amountField.positionCaret(amountField.getLength());
    }

    public void focusDescriptionAtStart() {
        descriptionField.requestFocus();
        descriptionField.positionCaret(0);
    }

    public void focusDescriptionAtEnd() {
        descriptionField.requestFocus();
        descriptionField.positionCaret(descriptionField.getLength());
    }

    private void wireListeners() {
        amountField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!suppressEvents) {
                actions.onRowContentChanged();
            }
        });

        descriptionField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!suppressEvents) {
                actions.onRowContentChanged();
            }
        });

        // Prevent a stray Space character from leaking into descriptionField after focus switch.
        amountField.addEventFilter(KeyEvent.KEY_TYPED, event -> {
            if (" ".equals(event.getCharacter())) {
                event.consume();
            }
        });

        amountField.setOnKeyPressed(this::handleAmountKeyPressed);
        descriptionField.setOnKeyPressed(this::handleDescriptionKeyPressed);
    }

    private void handleAmountKeyPressed(KeyEvent event) {
        KeyCode code = event.getCode();
        if (isDeleteShortcut(event)) {
            event.consume();
            actions.onDeleteRow(this, false);
            return;
        }

        switch (code) {
            case ENTER -> {
                event.consume();
                actions.onCreateRowAfter(this);
            }
            case SPACE -> {
                event.consume();
                Platform.runLater(this::focusDescriptionAtStart);
            }
            case RIGHT -> {
                if (amountField.getCaretPosition() == amountField.getLength()
                        && amountField.getSelection().getLength() == 0) {
                    event.consume();
                    focusDescriptionAtStart();
                }
            }
            case UP -> {
                event.consume();
                actions.onNavigateUp(this, true);
            }
            case DOWN -> {
                event.consume();
                actions.onNavigateDown(this, true);
            }
            case BACK_SPACE -> {
                if (amountField.getText().isEmpty() && descriptionField.getText().isEmpty()) {
                    event.consume();
                    actions.onDeleteRow(this, true);
                }
            }
            default -> {
            }
        }
    }

    private void handleDescriptionKeyPressed(KeyEvent event) {
        KeyCode code = event.getCode();
        if (isDeleteShortcut(event)) {
            event.consume();
            actions.onDeleteRow(this, false);
            return;
        }

        switch (code) {
            case ENTER -> {
                event.consume();
                actions.onCreateRowAfter(this);
            }
            case LEFT -> {
                if (descriptionField.getCaretPosition() == 0
                        && descriptionField.getSelection().getLength() == 0) {
                    event.consume();
                    focusAmountAtEnd();
                }
            }
            case BACK_SPACE -> {
                if (descriptionField.getText().isEmpty()) {
                    event.consume();
                    focusAmountAtEnd();
                }
            }
            case UP -> {
                event.consume();
                actions.onNavigateUp(this, false);
            }
            case DOWN -> {
                event.consume();
                actions.onNavigateDown(this, false);
            }
            default -> {
            }
        }
    }

    private static boolean isDeleteShortcut(KeyEvent event) {
        return event.isShortcutDown()
                && (event.getCode() == KeyCode.BACK_SPACE || event.getCode() == KeyCode.D);
    }
}
