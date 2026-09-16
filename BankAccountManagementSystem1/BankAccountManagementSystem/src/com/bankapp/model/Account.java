package com.bankapp.model;

import com.bankapp.exception.InsufficientFundsException;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a single bank account.
 *
 * NON-FUNCTIONAL REQUIREMENT: Reliability & thread safety — every method
 * that mutates the balance is {@code synchronized} on the account instance,
 * so concurrent transactions against the SAME account (e.g. a transfer
 * touching two accounts from different threads) cannot corrupt the balance
 * or interleave into an inconsistent state.
 */
public class Account implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum AccountType { SAVINGS, CURRENT }
    public enum Status { ACTIVE, FROZEN }

    private final String accountNumber;
    private final String customerId;
    private final AccountType accountType;
    private BigDecimal balance;
    private String pinHash;
    private String pinSalt;
    private Status status;
    private int failedLoginAttempts;
    private final LocalDateTime createdAt;
    private final List<Transaction> transactions;

    public Account(String accountNumber, String customerId, AccountType accountType,
                   BigDecimal openingBalance, String pinHash, String pinSalt) {
        this.accountNumber = accountNumber;
        this.customerId = customerId;
        this.accountType = accountType;
        this.balance = openingBalance;
        this.pinHash = pinHash;
        this.pinSalt = pinSalt;
        this.status = Status.ACTIVE;
        this.failedLoginAttempts = 0;
        this.createdAt = LocalDateTime.now();
        this.transactions = new ArrayList<>();
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getCustomerId() {
        return customerId;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public synchronized BigDecimal getBalance() {
        return balance;
    }

    public String getPinHash() {
        return pinHash;
    }

    public String getPinSalt() {
        return pinSalt;
    }

    public synchronized void updatePin(String newHash, String newSalt) {
        this.pinHash = newHash;
        this.pinSalt = newSalt;
    }

    public synchronized Status getStatus() {
        return status;
    }

    public synchronized void setStatus(Status status) {
        this.status = status;
    }

    public synchronized int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public synchronized void registerFailedLogin() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= 3) {
            this.status = Status.FROZEN;
        }
    }

    public synchronized void resetFailedLogins() {
        this.failedLoginAttempts = 0;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public synchronized List<Transaction> getTransactions() {
        return Collections.unmodifiableList(new ArrayList<>(transactions));
    }

    /**
     * Credits the account. Synchronized so concurrent credits/debits on the
     * same account are strictly serialized.
     */
    public synchronized void credit(BigDecimal amount, TransactionType type, String description) {
        this.balance = this.balance.add(amount);
        transactions.add(new Transaction(accountNumber, type, amount, balance, description));
    }

    /**
     * Debits the account after confirming sufficient funds.
     * Throws {@link InsufficientFundsException} rather than allowing an
     * overdraft, keeping balance invariants intact.
     */
    public synchronized void debit(BigDecimal amount, TransactionType type, String description)
            throws InsufficientFundsException {
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(accountNumber, balance.toPlainString(), amount.toPlainString());
        }
        this.balance = this.balance.subtract(amount);
        transactions.add(new Transaction(accountNumber, type, amount, balance, description));
    }

    @Override
    public String toString() {
        return String.format("Account[number=%s, type=%s, balance=%s, status=%s]",
                accountNumber, accountType, balance, status);
    }
}
