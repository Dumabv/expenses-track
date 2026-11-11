package com.expensestrack.ui;

import com.expensestrack.model.ExpenseSummary;
import com.expensestrack.service.ExpenseService;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.Objects;

public class MainView extends StackPane {

    private final ExpenseService expenseService;
    private final ObjectProperty<ExpenseSummary> summaryProperty;

    private final IncomeView incomeView;
    private final ExpenseListView expenseListView;
    private final SummaryView summaryView;
    private final ScrollPane scrollPane;
    private final Label statusLabel;

    public MainView(ExpenseService expenseService, String initialNotice) {
        this.expenseService = Objects.requireNonNull(expenseService, "ExpenseService must not be null");
        ExpenseSummary initialSummary = expenseService.getSummary();
        this.summaryProperty = new SimpleObjectProperty<>(initialSummary);

        getStyleClass().add("app-root");
        setAlignment(Pos.TOP_CENTER);
        setStyle("-fx-font-family: \"" + UiFormatting.resolveEditorFontFamily() + "\";");

        VBox editorShell = new VBox();
        editorShell.getStyleClass().add("editor-shell");
        editorShell.setMaxWidth(760);
        editorShell.setPadding(new Insets(36, 48, 24, 48));

        Label titleLabel = new Label("Expenses Track");
        titleLabel.getStyleClass().add("app-title");
        titleLabel.setStyle("-fx-font-family: \"" + UiFormatting.resolveTitleFontFamily() + "\";");
        VBox.setMargin(titleLabel, new Insets(0, 0, 22, 0));

        expenseListView = new ExpenseListView(
                expenseService::replaceExpenses,
                this::focusIncomeFromList,
                this::scrollToBottom
        );

        incomeView = new IncomeView(
                expenseService::updateIncomeAmount,
                expenseService::updateIncomeCurrency,
                expenseListView::focusFirstRow
        );
        VBox.setMargin(incomeView, new Insets(0, 0, 18, 0));

        Label expensesLabel = new Label("Expenses:");
        expensesLabel.getStyleClass().add("section-label");
        VBox.setMargin(expensesLabel, new Insets(0, 0, 8, 0));

        summaryView = new SummaryView();
        summaryView.bind(summaryProperty);

        VBox documentBody = new VBox(expenseListView, summaryView);
        documentBody.getStyleClass().add("editor-shell");
        documentBody.setPadding(new Insets(0, 4, 12, 0));
        documentBody.setOnMouseClicked(event -> {
            if (event.getTarget() == documentBody) {
                expenseListView.focusLastRow();
            }
        });

        scrollPane = new ScrollPane(documentBody);
        scrollPane.getStyleClass().add("editor-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        statusLabel = new Label();
        statusLabel.getStyleClass().add("status-notice");
        statusLabel.setWrapText(true);
        VBox.setMargin(statusLabel, new Insets(8, 0, 0, 0));
        setNotice(initialNotice);

        editorShell.getChildren().addAll(titleLabel, incomeView, expensesLabel, scrollPane, statusLabel);
        getChildren().add(editorShell);

        populateInitialState(initialSummary);
        wireServiceUpdates();
    }

    public void focusInitialField() {
        ExpenseSummary current = expenseService.getSummary();
        boolean isBlankState = current.income().amount().compareTo(BigDecimal.ZERO) == 0
                && current.lines().isEmpty();
        Platform.runLater(() -> {
            if (isBlankState) {
                incomeView.focusAmount();
            } else {
                expenseListView.focusLastRow();
            }
        });
    }

    public void showNotice(String message) {
        if (Platform.isFxApplicationThread()) {
            setNotice(message);
        } else {
            Platform.runLater(() -> setNotice(message));
        }
    }

    private void populateInitialState(ExpenseSummary initialSummary) {
        incomeView.setIncome(initialSummary.income());
        expenseListView.setExpenses(expenseService.getExpenses(), initialSummary);
    }

    private void wireServiceUpdates() {
        summaryProperty.addListener((obs, oldSummary, newSummary) -> {
            if (newSummary != null) {
                expenseListView.updateSummary(newSummary);
            }
        });

        expenseService.addSummaryListener(updatedSummary -> {
            if (Platform.isFxApplicationThread()) {
                summaryProperty.set(updatedSummary);
            } else {
                Platform.runLater(() -> summaryProperty.set(updatedSummary));
            }
        });
    }

    private void focusIncomeFromList() {
        incomeView.focusAmount();
    }

    private void scrollToBottom() {
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }

    private void setNotice(String message) {
        boolean visible = message != null && !message.isBlank();
        statusLabel.setText(visible ? message : "");
        statusLabel.setVisible(visible);
        statusLabel.setManaged(visible);
    }
}
