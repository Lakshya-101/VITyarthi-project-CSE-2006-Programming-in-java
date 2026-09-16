package com.bankapp.exception;

/** Thrown when login/authentication fails (wrong PIN, locked account, etc.). */
public class AuthenticationException extends BankException {

    public AuthenticationException(String message) {
        super(message);
    }
}
