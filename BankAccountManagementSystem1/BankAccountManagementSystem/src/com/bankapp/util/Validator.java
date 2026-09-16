package com.bankapp.util;

import com.bankapp.exception.InvalidAmountException;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Centralized validation rules.
 * NON-FUNCTIONAL REQUIREMENT: Reliability — every entry point into the
 * system funnels through here first, so bad input is rejected early with
 * a clear message instead of causing inconsistent state deeper in the code.
 */
public final class Validator {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PIN_PATTERN = Pattern.compile("^\\d{4,6}$");
    private static final BigDecimal MAX_TRANSACTION_LIMIT = new BigDecimal("1000000");

    private Validator() {
        // utility class, no instances
    }

    public static void validateAmount(BigDecimal amount) throws InvalidAmountException {
        if (amount == null) {
            throw new InvalidAmountException("Amount cannot be null.");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero.");
        }
        if (amount.compareTo(MAX_TRANSACTION_LIMIT) > 0) {
            throw new InvalidAmountException(
                    "Amount exceeds the maximum allowed transaction limit of " + MAX_TRANSACTION_LIMIT);
        }
    }

    public static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty.");
        }
    }

    public static void validateEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email address: " + email);
        }
    }

    public static void validatePin(String pin) {
        if (pin == null || !PIN_PATTERN.matcher(pin).matches()) {
            throw new IllegalArgumentException("PIN must be 4 to 6 digits.");
        }
    }
}
