package com.bankapp.service;

import com.bankapp.exception.AccountNotFoundException;
import com.bankapp.exception.InsufficientFundsException;
import com.bankapp.exception.InvalidAmountException;
import com.bankapp.model.Account;
import com.bankapp.model.TransactionType;
import com.bankapp.util.FileLogger;
import com.bankapp.util.Validator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * MODULE 2: Deposits, withdrawals, transfers, and month-end interest posting.
 *
 * NON-FUNCTIONAL REQUIREMENT: Scalability — {@link #applyMonthlyInterestToAll()}
 * uses a fixed thread pool to post interest across every account in parallel
 * instead of a single sequential loop, so the batch job scales as the
 * customer base grows instead of becoming a linear bottleneck.
 */
public class AccountService {

    private static final BigDecimal SAVINGS_ANNUAL_RATE = new BigDecimal("0.04"); // 4% p.a.
    private static final BigDecimal MONTHLY_DIVISOR = new BigDecimal("12");

    private final BankRepository repository;
    private final FraudDetectionService fraudDetectionService;

    public AccountService(BankRepository repository, FraudDetectionService fraudDetectionService) {
        this.repository = repository;
        this.fraudDetectionService = fraudDetectionService;
    }

    private Account requireAccount(String accountNumber) throws AccountNotFoundException {
        Account account = repository.getAccount(accountNumber);
        if (account == null) {
            throw new AccountNotFoundException(accountNumber);
        }
        return account;
    }

    public void deposit(String accountNumber, BigDecimal amount)
            throws AccountNotFoundException, InvalidAmountException {
        Validator.validateAmount(amount);
        Account account = requireAccount(accountNumber);
        account.credit(amount, TransactionType.DEPOSIT, "Cash deposit");
        FileLogger.info(String.format("Deposit of %s to %s", amount, accountNumber));
    }

    public void withdraw(String accountNumber, BigDecimal amount)
            throws AccountNotFoundException, InvalidAmountException, InsufficientFundsException {
        Validator.validateAmount(amount);
        Account account = requireAccount(accountNumber);

        fraudDetectionService.checkWithdrawal(account, amount);

        account.debit(amount, TransactionType.WITHDRAWAL, "Cash withdrawal");
        FileLogger.info(String.format("Withdrawal of %s from %s", amount, accountNumber));
    }

    /**
     * Transfers funds between two accounts.
     * Locks are acquired on both account objects in a fixed, deterministic
     * order (lower account number first) to prevent deadlocks that could
     * otherwise occur if two transfers happened between the same pair of
     * accounts in opposite directions on different threads.
     */
    public void transfer(String fromAccountNumber, String toAccountNumber, BigDecimal amount)
            throws AccountNotFoundException, InvalidAmountException, InsufficientFundsException {
        Validator.validateAmount(amount);
        if (fromAccountNumber.equals(toAccountNumber)) {
            throw new InvalidAmountException("Cannot transfer to the same account.");
        }
        Account from = requireAccount(fromAccountNumber);
        Account to = requireAccount(toAccountNumber);

        fraudDetectionService.checkWithdrawal(from, amount);

        Account first = fromAccountNumber.compareTo(toAccountNumber) < 0 ? from : to;
        Account second = fromAccountNumber.compareTo(toAccountNumber) < 0 ? to : from;

        synchronized (first) {
            synchronized (second) {
                from.debit(amount, TransactionType.TRANSFER_OUT, "Transfer to " + toAccountNumber);
                to.credit(amount, TransactionType.TRANSFER_IN, "Transfer from " + fromAccountNumber);
            }
        }
        FileLogger.info(String.format("Transfer of %s from %s to %s", amount, fromAccountNumber, toAccountNumber));
    }

    public BigDecimal getBalance(String accountNumber) throws AccountNotFoundException {
        return requireAccount(accountNumber).getBalance();
    }

    /**
     * Posts monthly interest to every SAVINGS account concurrently using a
     * fixed thread pool, then blocks until all postings complete.
     * Demonstrates safe concurrent mutation of shared state (each Account's
     * own synchronized methods make this safe even though many threads
     * touch the shared account map at once).
     */
    public int applyMonthlyInterestToAll() {
        Map<String, Account> accounts = repository.getAllAccounts();
        int poolSize = Math.max(2, Math.min(8, accounts.size()));
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        int[] postedCount = {0};

        for (Account account : accounts.values()) {
            executor.submit(() -> {
                if (account.getAccountType() == Account.AccountType.SAVINGS
                        && account.getStatus() == Account.Status.ACTIVE) {
                    BigDecimal monthlyRate = SAVINGS_ANNUAL_RATE.divide(MONTHLY_DIVISOR, 8, RoundingMode.HALF_UP);
                    BigDecimal interest = account.getBalance()
                            .multiply(monthlyRate)
                            .setScale(2, RoundingMode.HALF_UP);
                    if (interest.compareTo(BigDecimal.ZERO) > 0) {
                        account.credit(interest, TransactionType.INTEREST_CREDIT, "Monthly interest");
                        synchronized (postedCount) {
                            postedCount[0]++;
                        }
                        FileLogger.info("Interest of " + interest + " posted to " + account.getAccountNumber());
                    }
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            FileLogger.error("Interest posting job interrupted", e);
        }
        return postedCount[0];
    }
}
