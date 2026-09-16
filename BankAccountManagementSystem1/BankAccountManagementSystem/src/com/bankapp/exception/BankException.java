package com.bankapp.exception;

/**
 * Base checked exception for all banking domain errors.
 * Forces callers to explicitly handle business-rule violations
 * rather than letting them surface as unchecked runtime failures.
 */
public class BankException extends Exception {

    public BankException(String message) {
        super(message);
    }

    public BankException(String message, Throwable cause) {
        super(message, cause);
    }
}
