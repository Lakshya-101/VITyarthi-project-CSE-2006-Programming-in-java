package com.bankapp.service;

import com.bankapp.model.Account;
import com.bankapp.model.Transaction;
import com.bankapp.util.FileLogger;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * MODULE 3 (part A): Rule-based fraud-check simulation.
 *
 * This does not call any external service — it is a simplified simulation
 * for coursework purposes, applying two common real-world heuristics:
 *   1. A single withdrawal larger than a fixed threshold OR more than
 *      half the account's current balance is flagged as high-risk.
 *   2. More than {@code VELOCITY_LIMIT} withdrawals/transfers within the
 *      last {@code VELOCITY_WINDOW_MINUTES} minutes is flagged as
 *      suspicious "velocity" (rapid-fire transaction) behaviour.
 *
 * Flags do not block the transaction outright (to keep the demo usable);
 * they are logged as warnings so a real system could route them to manual
 * review or a hard block.
 */
public class FraudDetectionService {

    private static final BigDecimal LARGE_WITHDRAWAL_THRESHOLD = new BigDecimal("50000");
    private static final int VELOCITY_LIMIT = 4;
    private static final int VELOCITY_WINDOW_MINUTES = 10;

    public void checkWithdrawal(Account account, BigDecimal amount) {
        boolean isLarge = amount.compareTo(LARGE_WITHDRAWAL_THRESHOLD) > 0
                || amount.compareTo(account.getBalance().divide(new BigDecimal("2"), java.math.RoundingMode.HALF_UP)) > 0;

        if (isLarge) {
            FileLogger.warn("FRAUD ALERT [large amount]: account " + account.getAccountNumber()
                    + " attempted debit of " + amount);
            System.out.println(">> Notice: this transaction was flagged for manual review (large amount), "
                    + "but has been allowed to proceed for demo purposes.");
        }

        long recentCount = countRecentDebits(account);
        if (recentCount >= VELOCITY_LIMIT) {
            FileLogger.warn("FRAUD ALERT [velocity]: account " + account.getAccountNumber()
                    + " has made " + recentCount + " debit transactions in the last "
                    + VELOCITY_WINDOW_MINUTES + " minutes");
            System.out.println(">> Notice: unusually frequent transactions detected on this account.");
        }
    }

    private long countRecentDebits(Account account) {
        LocalDateTime cutoff = LocalDateTime.now().minus(VELOCITY_WINDOW_MINUTES, ChronoUnit.MINUTES);
        List<Transaction> transactions = account.getTransactions();
        return transactions.stream()
                .filter(t -> t.getTimestamp().isAfter(cutoff))
                .filter(t -> t.getType() == com.bankapp.model.TransactionType.WITHDRAWAL
                        || t.getType() == com.bankapp.model.TransactionType.TRANSFER_OUT)
                .count();
    }

    /** Scans every account for a suspiciously high number of stored flags (used by admin menu). */
    public void scanAllAccounts(Iterable<Account> accounts) {
        System.out.println("Running fraud scan across all accounts...");
        for (Account account : accounts) {
            long recentCount = countRecentDebits(account);
            if (recentCount >= VELOCITY_LIMIT) {
                System.out.println("  [FLAGGED] " + account.getAccountNumber()
                        + " — " + recentCount + " rapid debit transactions");
            }
        }
        System.out.println("Fraud scan complete.");
    }
}
