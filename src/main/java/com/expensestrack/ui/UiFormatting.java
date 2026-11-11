package com.expensestrack.ui;

import com.expensestrack.service.AmountFormatter;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.List;
import java.util.Set;

/**
 * Shared UI helpers for numeric input filtering, auto-sizing editor fields, and font detection.
 */
final class UiFormatting {

    private static final List<String> PREFERRED_MONO_FONTS = List.of(
            "JetBrains Mono",
            "SF Mono",
            "Cascadia Mono",
            "Menlo",
            "Consolas",
            "DejaVu Sans Mono",
            "Liberation Mono"
    );

    private static final List<String> PREFERRED_UI_FONTS = List.of(
            "SF Pro Text",
            "Inter",
            "Segoe UI",
            "Helvetica Neue",
            "Ubuntu"
    );

    private UiFormatting() {
    }

    static String resolveEditorFontFamily() {
        return pickInstalledFont(PREFERRED_MONO_FONTS, "Monospaced");
    }

    static String resolveTitleFontFamily() {
        return pickInstalledFont(PREFERRED_UI_FONTS, "System");
    }

    static void applyNumericFilter(TextField field) {
        field.setTextFormatter(new TextFormatter<>(change -> {
            if (!change.isContentChange()) {
                return change;
            }
            return AmountFormatter.isValidEditableInput(change.getControlNewText()) ? change : null;
        }));
    }

    static void makeAutoSizing(TextField field, double minWidth, double maxWidth) {
        Text measurer = new Text();
        measurer.fontProperty().bind(field.fontProperty());

        Runnable updateWidth = () -> {
            String text = field.getText();
            String sample = (text == null || text.isEmpty()) ? field.getPromptText() : text;
            measurer.setText(sample == null ? "" : sample);
            double measured = Math.ceil(measurer.getLayoutBounds().getWidth()) + 14;
            double clamped = Math.max(minWidth, Math.min(maxWidth, measured));
            field.setPrefWidth(clamped);
            field.setMinWidth(clamped);
            field.setMaxWidth(clamped);
        };

        field.textProperty().addListener((obs, oldVal, newVal) -> updateWidth.run());
        field.fontProperty().addListener((obs, oldFont, newFont) -> updateWidth.run());
        updateWidth.run();
    }

    private static String pickInstalledFont(List<String> candidates, String fallback) {
        Set<String> installed = Set.copyOf(Font.getFamilies());
        for (String candidate : candidates) {
            if (installed.contains(candidate)) {
                return candidate;
            }
        }
        return fallback;
    }
}
