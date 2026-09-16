package com.bankapp.exception;

/** Thrown when a monetary amount fails validation (negative, zero, or exceeds limits). */
public class InvalidAmountException extends BankException {

    public InvalidAmountException(String message) {
        super(message);
    }
}
