package com.bankapp.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * An immutable record of a single ledger entry against an account.
 * Once created, a transaction is never modified — corrections are made
 * by adding new offsetting entries, mirroring how real ledgers work.
 */
public class Transaction implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String transactionId;
    private final String accountNumber;
    private final TransactionType type;
    private final BigDecimal amount;
    private final BigDecimal balanceAfter;
    private final LocalDateTime timestamp;
    private final String description;

    public Transaction(String accountNumber, TransactionType type, BigDecimal amount,
                        BigDecimal balanceAfter, String description) {
        this.transactionId = UUID.randomUUID().toString();
        this.accountNumber = accountNumber;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.timestamp = LocalDateTime.now();
        this.description = description;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getDescription() {
        return description;
    }

    /** A single formatted line suitable for printing in a statement. */
    public String toStatementLine() {
        return String.format("%-19s | %-13s | %10s | Balance: %10s | %s",
                timestamp.format(FORMATTER), type, amount, balanceAfter, description);
    }

    @Override
    public String toString() {
        return toStatementLine();
    }
}
