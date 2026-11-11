package com.expensestrack;

import javafx.application.Application;

/**
 * Non-JavaFX entry point used when launching from a packaged JAR or jpackage image.
 * Keeping {@code main} separate from {@link ExpensesTrackApp} avoids JavaFX module-path errors.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Application.launch(ExpensesTrackApp.class, args);
    }
}
