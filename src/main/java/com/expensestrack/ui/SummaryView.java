package com.expensestrack.ui;

import com.expensestrack.model.ExpenseSummary;
import com.expensestrack.service.AmountFormatter;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.Objects;

public class SummaryView extends VBox {

    private static final String NEGATIVE_STYLE_CLASS = "summary-rest-negative";

    private final Label totalLabel;
    private final Label restLabel;

    public SummaryView() {
        setSpacing(6);
        setPadding(new Insets(18, 0, 8, 0));

        Region divider = new Region();
        divider.getStyleClass().add("summary-divider");
        VBox.setMargin(divider, new Insets(0, 0, 12, 0));

        totalLabel = new Label();
        totalLabel.getStyleClass().add("summary-total-label");

        restLabel = new Label();
        restLabel.getStyleClass().add("summary-rest-label");

        getChildren().addAll(divider, totalLabel, restLabel);
    }

    public void bind(ObservableValue<ExpenseSummary> summaryProperty) {
        Objects.requireNonNull(summaryProperty, "summaryProperty must not be null");
        if (summaryProperty.getValue() != null) {
            render(summaryProperty.getValue());
        }
        summaryProperty.addListener((obs, oldSummary, newSummary) -> {
            if (newSummary != null) {
                render(newSummary);
            }
        });
    }

    public void render(ExpenseSummary summary) {
        Objects.requireNonNull(summary, "Summary must not be null");
        String currency = summary.income().currency();

        String totalMoney = AmountFormatter.formatMoney(summary.totalExpenses(), currency);
        String totalPercent = AmountFormatter.formatPercentage(summary.totalPercentage());
        totalLabel.setText("TOTAL: " + totalMoney + " = " + totalPercent);

        String restMoney = AmountFormatter.formatMoney(summary.rest(), currency);
        String restPercent = AmountFormatter.formatPercentage(summary.restPercentage());
        restLabel.setText("Rest: " + restMoney + " = " + restPercent);

        boolean isNegative = summary.rest().compareTo(BigDecimal.ZERO) < 0;
        if (isNegative) {
            if (!restLabel.getStyleClass().contains(NEGATIVE_STYLE_CLASS)) {
                restLabel.getStyleClass().add(NEGATIVE_STYLE_CLASS);
            }
        } else {
            restLabel.getStyleClass().remove(NEGATIVE_STYLE_CLASS);
        }
    }
}
