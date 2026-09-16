package com.bankapp.exception;

/** Thrown when a debit operation would take an account below its allowed balance. */
public class InsufficientFundsException extends BankException {

    public InsufficientFundsException(String accountNumber, String available, String requested) {
        super(String.format(
                "Insufficient funds in account %s. Available: %s, Requested: %s",
                accountNumber, available, requested));
    }
}
