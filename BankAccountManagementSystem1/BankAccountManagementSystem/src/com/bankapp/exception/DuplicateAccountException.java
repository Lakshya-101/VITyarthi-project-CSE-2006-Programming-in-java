package com.bankapp.exception;

/** Thrown when trying to create an account/customer record that already exists. */
public class DuplicateAccountException extends BankException {

    public DuplicateAccountException(String message) {
        super(message);
    }
}
