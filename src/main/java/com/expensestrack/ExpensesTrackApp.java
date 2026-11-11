package com.expensestrack;

import atlantafx.base.theme.PrimerDark;
import com.expensestrack.service.CalculationService;
import com.expensestrack.service.ExpenseService;
import com.expensestrack.storage.AutoSaveCoordinator;
import com.expensestrack.storage.ExpenseStorage;
import com.expensestrack.storage.JsonExpenseStorage;
import com.expensestrack.storage.LoadResult;
import com.expensestrack.storage.TrackerData;
import com.expensestrack.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;

public class ExpensesTrackApp extends Application {

    private static final System.Logger LOGGER = System.getLogger(ExpensesTrackApp.class.getName());
    private static final String STYLESHEET_PATH = "/styles/expenses-track.css";
    private static final String ICON_PATH = "/icons/app-icon.png";

    private AutoSaveCoordinator autoSaveCoordinator;

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        ExpenseStorage storage = JsonExpenseStorage.createDefault();
        LoadResult loadResult = storage.load();
        TrackerData initialData = loadResult.data();

        ExpenseService expenseService = new ExpenseService(
                new CalculationService(),
                initialData.income(),
                initialData.expenses()
        );

        MainView mainView = new MainView(expenseService, loadResult.noticeMessage());

        autoSaveCoordinator = new AutoSaveCoordinator(
                storage,
                Duration.ofMillis(400),
                error -> {
                    LOGGER.log(System.Logger.Level.ERROR, "Failed to auto-save expenses", error);
                    mainView.showNotice("Could not save changes to disk: " + error.getMessage());
                }
        );

        expenseService.addSummaryListener(summary ->
                autoSaveCoordinator.scheduleSave(TrackerData.fromSummary(summary)));

        Scene scene = new Scene(mainView, 700, 620);
        URL stylesheet = ExpensesTrackApp.class.getResource(STYLESHEET_PATH);
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        } else {
            LOGGER.log(System.Logger.Level.WARNING, "Stylesheet not found: {0}", STYLESHEET_PATH);
        }

        stage.setTitle("Expenses Track");
        stage.setMinWidth(460);
        stage.setMinHeight(380);
        applyAppIcon(stage);
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> shutdownAutoSave());
        stage.show();

        mainView.focusInitialField();
    }

    @Override
    public void stop() {
        shutdownAutoSave();
    }

    private void shutdownAutoSave() {
        if (autoSaveCoordinator != null) {
            autoSaveCoordinator.close();
        }
    }

    private void applyAppIcon(Stage stage) {
        try (InputStream fxIconStream = ExpensesTrackApp.class.getResourceAsStream(ICON_PATH)) {
            if (fxIconStream != null) {
                stage.getIcons().add(new Image(fxIconStream));
            } else {
                LOGGER.log(System.Logger.Level.WARNING, "Icon resource not found: {0}", ICON_PATH);
            }
        } catch (IOException ex) {
            LOGGER.log(System.Logger.Level.WARNING, "Could not load window icon", ex);
        }
    }
}
