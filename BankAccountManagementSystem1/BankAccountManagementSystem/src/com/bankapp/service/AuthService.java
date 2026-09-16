package com.bankapp.service;

import com.bankapp.exception.AuthenticationException;
import com.bankapp.exception.DuplicateAccountException;
import com.bankapp.exception.InvalidAmountException;
import com.bankapp.model.Account;
import com.bankapp.model.Customer;
import com.bankapp.util.FileLogger;
import com.bankapp.util.PasswordUtil;
import com.bankapp.util.Validator;

import java.math.BigDecimal;

/**
 * MODULE 1: Customer registration, account creation and PIN-based authentication.
 * NON-FUNCTIONAL REQUIREMENT: Security — PINs are salted + hashed (never
 * stored or compared in plain text), and accounts are auto-frozen after
 * three consecutive failed login attempts to resist brute-force guessing.
 */
public class AuthService {

    private final BankRepository repository;

    public AuthService(BankRepository repository) {
        this.repository = repository;
    }

    /** Registers a brand-new customer record (does not create an account yet). */
    public Customer registerCustomer(String name, String email, String phone) {
        Validator.validateName(name);
        Validator.validateEmail(email);

        String customerId = repository.nextCustomerId();
        Customer customer = new Customer(customerId, name, email, phone);
        repository.addCustomer(customer);
        FileLogger.info("Registered new customer: " + customerId + " (" + name + ")");
        return customer;
    }

    /** Opens a new account for an existing customer, protected by a PIN. */
    public Account openAccount(String customerId, Account.AccountType type,
                                BigDecimal openingBalance, String pin)
            throws InvalidAmountException, DuplicateAccountException {
        Customer customer = repository.getCustomer(customerId);
        if (customer == null) {
            throw new DuplicateAccountException("Cannot open account: customer " + customerId + " does not exist.");
        }
        if (openingBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidAmountException("Opening balance cannot be negative.");
        }
        Validator.validatePin(pin);

        String accountNumber = repository.nextAccountNumber();
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hash(pin, salt);

        Account account = new Account(accountNumber, customerId, type, openingBalance, hash, salt);
        repository.addAccount(account);
        customer.linkAccount(accountNumber);

        FileLogger.info("Opened account " + accountNumber + " for customer " + customerId);
        return account;
    }

    /**
     * Authenticates a login attempt. Locks the account (FROZEN) after three
     * consecutive failures, which is itself a fraud-mitigation measure.
     */
    public Account login(String accountNumber, String pin) throws AuthenticationException {
        Account account = repository.getAccount(accountNumber);
        if (account == null) {
            FileLogger.warn("Login attempt on unknown account: " + accountNumber);
            throw new AuthenticationException("Account not found.");
        }
        if (account.getStatus() == Account.Status.FROZEN) {
            FileLogger.warn("Login attempt on frozen account: " + accountNumber);
            throw new AuthenticationException(
                    "Account is frozen due to repeated failed login attempts. Contact support.");
        }
        boolean valid = PasswordUtil.verify(pin, account.getPinSalt(), account.getPinHash());
        if (!valid) {
            account.registerFailedLogin();
            FileLogger.warn("Failed login attempt #" + account.getFailedLoginAttempts()
                    + " on account: " + accountNumber);
            throw new AuthenticationException("Incorrect PIN.");
        }
        account.resetFailedLogins();
        FileLogger.info("Successful login: " + accountNumber);
        return account;
    }

    public void changePin(Account account, String newPin) {
        Validator.validatePin(newPin);
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hash(newPin, salt);
        account.updatePin(hash, salt);
        FileLogger.info("PIN changed for account: " + account.getAccountNumber());
    }
}
