package com.expensestrack.model;

import java.math.BigDecimal;
import java.util.Objects;

public record Income(BigDecimal amount, String currency) {

    public static final String DEFAULT_CURRENCY = "$";

    public Income {
        Objects.requireNonNull(amount, "Income amount must not be null");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Income amount cannot be negative: " + amount);
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency must not be blank");
        }
        currency = currency.trim();
    }

    public static Income empty() {
        return new Income(BigDecimal.ZERO, DEFAULT_CURRENCY);
    }

    public static Income of(BigDecimal amount, String currency) {
        return new Income(amount, currency);
    }

    public Income withAmount(BigDecimal newAmount) {
        return new Income(newAmount, currency);
    }

    public Income withCurrency(String newCurrency) {
        return new Income(amount, newCurrency);
    }
}
