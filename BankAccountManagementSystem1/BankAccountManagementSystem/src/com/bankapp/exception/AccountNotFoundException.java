package com.bankapp.exception;

/** Thrown when an operation references an account number that does not exist. */
public class AccountNotFoundException extends BankException {

    public AccountNotFoundException(String accountNumber) {
        super("No account found with account number: " + accountNumber);
    }
}
