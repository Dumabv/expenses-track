package com.expensestrack.ui;

import com.expensestrack.model.Income;
import com.expensestrack.service.AmountFormatter;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.function.Consumer;

public class IncomeView extends HBox {

    private static final String[] DEFAULT_CURRENCIES = {
            "$", "€", "£", "CHF", "RON", "MDL", "PLN", "CAD", "JPY"
    };

    private final TextField amountField;
    private final ComboBox<String> currencyCombo;

    private boolean suppressEvents;
    private String lastNotifiedCurrency = Income.DEFAULT_CURRENCY;

    public IncomeView(
            Consumer<BigDecimal> onAmountChanged,
            Consumer<String> onCurrencyChanged,
            Runnable onNavigateDown
    ) {
        Objects.requireNonNull(onAmountChanged, "onAmountChanged must not be null");
        Objects.requireNonNull(onCurrencyChanged, "onCurrencyChanged must not be null");
        Objects.requireNonNull(onNavigateDown, "onNavigateDown must not be null");

        setAlignment(Pos.CENTER_LEFT);
        setSpacing(8);

        Label incomeLabel = new Label("Income:");
        incomeLabel.getStyleClass().add("section-label");

        amountField = new TextField();
        amountField.setPromptText("0");
        amountField.getStyleClass().addAll("editor-field", "amount-field");
        UiFormatting.applyNumericFilter(amountField);
        UiFormatting.makeAutoSizing(amountField, 38, 180);

        currencyCombo = new ComboBox<>();
        currencyCombo.getItems().addAll(DEFAULT_CURRENCIES);
        currencyCombo.setEditable(true);
        currencyCombo.setValue(Income.DEFAULT_CURRENCY);
        currencyCombo.setPrefWidth(72);
        currencyCombo.setMinWidth(68);
        currencyCombo.getStyleClass().add("currency-combo");

        getChildren().addAll(incomeLabel, amountField, currencyCombo);

        wireEvents(onAmountChanged, onCurrencyChanged, onNavigateDown);
    }

    public void setIncome(Income income) {
        Objects.requireNonNull(income, "Income must not be null");
        suppressEvents = true;
        try {
            if (income.amount().compareTo(BigDecimal.ZERO) == 0) {
                amountField.clear();
            } else {
                amountField.setText(AmountFormatter.formatAmount(income.amount()));
            }
            currencyCombo.setValue(income.currency());
            currencyCombo.getEditor().setText(income.currency());
            lastNotifiedCurrency = income.currency();
        } finally {
            suppressEvents = false;
        }
    }

    public void focusAmount() {
        amountField.requestFocus();
        amountField.positionCaret(amountField.getLength());
    }

    private void wireEvents(
            Consumer<BigDecimal> onAmountChanged,
            Consumer<String> onCurrencyChanged,
            Runnable onNavigateDown
    ) {
        amountField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (suppressEvents) {
                return;
            }
            BigDecimal parsed = AmountFormatter.parseNonNegativeAmount(newVal).orElse(BigDecimal.ZERO);
            onAmountChanged.accept(parsed);
        });

        amountField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.DOWN) {
                event.consume();
                onNavigateDown.run();
            }
        });

        currencyCombo.valueProperty().addListener((obs, oldVal, newVal) ->
                notifyCurrencyIfValid(newVal, onCurrencyChanged));

        currencyCombo.getEditor().textProperty().addListener((obs, oldVal, newVal) ->
                notifyCurrencyIfValid(newVal, onCurrencyChanged));

        currencyCombo.getEditor().focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && !suppressEvents) {
                String current = currencyCombo.getEditor().getText();
                if (current == null || current.isBlank()) {
                    suppressEvents = true;
                    try {
                        currencyCombo.setValue(Income.DEFAULT_CURRENCY);
                        currencyCombo.getEditor().setText(Income.DEFAULT_CURRENCY);
                    } finally {
                        suppressEvents = false;
                    }
                    onCurrencyChanged.accept(Income.DEFAULT_CURRENCY);
                    lastNotifiedCurrency = Income.DEFAULT_CURRENCY;
                }
            }
        });

        currencyCombo.getEditor().setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                String text = currencyCombo.getEditor().getText();
                if (text != null && !text.isBlank()) {
                    currencyCombo.setValue(text.trim());
                }
                event.consume();
                onNavigateDown.run();
            }
        });
    }

    private void notifyCurrencyIfValid(String rawCurrency, Consumer<String> onCurrencyChanged) {
        if (suppressEvents || rawCurrency == null || rawCurrency.isBlank()) {
            return;
        }
        String trimmed = rawCurrency.trim();
        if (trimmed.equals(lastNotifiedCurrency)) {
            return;
        }
        lastNotifiedCurrency = trimmed;
        onCurrencyChanged.accept(trimmed);
    }
}
