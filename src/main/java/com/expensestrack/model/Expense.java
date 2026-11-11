package com.expensestrack.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record Expense(String id, BigDecimal amount, String description) {

    public Expense {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Expense id must not be blank");
        }
        Objects.requireNonNull(amount, "Expense amount must not be null");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Expense amount cannot be negative: " + amount);
        }
        description = description == null ? "" : description;
    }

    public static Expense of(BigDecimal amount, String description) {
        return new Expense(UUID.randomUUID().toString(), amount, description);
    }
}
