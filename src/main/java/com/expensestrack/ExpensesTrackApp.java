package com.expensestrack;

import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class ExpensesTrackApp extends Application {

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        Label placeholder = new Label("Expenses Track");
        StackPane root = new StackPane(placeholder);
        root.setStyle("-fx-background-color: #1E1E1E;");

        Scene scene = new Scene(root, 680, 620);
        stage.setTitle("Expenses Track");
        stage.setMinWidth(460);
        stage.setMinHeight(380);
        stage.setScene(scene);
        stage.show();
    }
}
