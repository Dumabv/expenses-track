package com.expensestrack.service;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public final class AmountFormatter {

    private static final Pattern VALID_NON_NEGATIVE_NUMBER = Pattern.compile("^(\\d+(\\.\\d*)?|\\.\\d+)$");

    private AmountFormatter() {
    }

    public static Optional<BigDecimal> parseNonNegativeAmount(String rawText) {
        if (rawText == null) {
            return Optional.empty();
        }
        String normalized = normalizeDecimalInput(rawText);
        if (normalized.isEmpty() || !VALID_NON_NEGATIVE_NUMBER.matcher(normalized).matches()) {
            return Optional.empty();
        }
        return Optional.of(new BigDecimal(normalized));
    }

    public static boolean isValidEditableInput(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return true;
        }
        String normalized = normalizeDecimalInput(rawText);
        return ".".equals(normalized) || VALID_NON_NEGATIVE_NUMBER.matcher(normalized).matches();
    }

    public static String formatAmount(BigDecimal amount) {
        Objects.requireNonNull(amount, "Amount must not be null");
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            return "0";
        }
        return amount.stripTrailingZeros().toPlainString();
    }

    public static String formatMoney(BigDecimal amount, String currency) {
        Objects.requireNonNull(amount, "Amount must not be null");
        String safeCurrency = (currency == null || currency.isBlank()) ? "$" : currency.trim();
        String formattedAmount = formatAmount(amount);
        if (safeCurrency.length() == 1) {
            return formattedAmount + safeCurrency;
        }
        return formattedAmount + " " + safeCurrency;
    }

    public static String formatPercentage(BigDecimal percentage) {
        Objects.requireNonNull(percentage, "Percentage must not be null");
        if (percentage.compareTo(BigDecimal.ZERO) == 0) {
            return "0%";
        }
        return percentage.stripTrailingZeros().toPlainString() + "%";
    }

    public static String formatExpenseComment(BigDecimal percentage) {
        return "// - " + formatPercentage(percentage);
    }

    private static String normalizeDecimalInput(String rawText) {
        String trimmed = rawText.trim();
        if (trimmed.indexOf(',') >= 0 && trimmed.indexOf('.') < 0) {
            return trimmed.replace(',', '.');
        }
        return trimmed;
    }
}
