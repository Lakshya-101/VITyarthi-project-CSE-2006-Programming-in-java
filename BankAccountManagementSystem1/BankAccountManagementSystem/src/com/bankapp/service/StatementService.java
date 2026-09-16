package com.bankapp.service;

import com.bankapp.exception.AccountNotFoundException;
import com.bankapp.model.Account;
import com.bankapp.model.Transaction;
import com.bankapp.util.FileLogger;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * MODULE 3 (part B): Statement generation and reporting.
 * Produces both a quick on-screen mini-statement and a full statement
 * exported to a text file on disk (demonstrating file I/O output).
 */
public class StatementService {

    private static final DateTimeFormatter HEADER_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final BankRepository repository;

    public StatementService(BankRepository repository) {
        this.repository = repository;
    }

    private Account requireAccount(String accountNumber) throws AccountNotFoundException {
        Account account = repository.getAccount(accountNumber);
        if (account == null) {
            throw new AccountNotFoundException(accountNumber);
        }
        return account;
    }

    /** Prints the last N transactions to the console. */
    public void printMiniStatement(String accountNumber, int lastN) throws AccountNotFoundException {
        Account account = requireAccount(accountNumber);
        List<Transaction> all = account.getTransactions();
        int fromIndex = Math.max(0, all.size() - lastN);
        List<Transaction> recent = all.subList(fromIndex, all.size());

        System.out.println("---- Mini Statement: " + accountNumber + " ----");
        if (recent.isEmpty()) {
            System.out.println("No transactions yet.");
        } else {
            for (Transaction t : recent) {
                System.out.println(t.toStatementLine());
            }
        }
        System.out.println("Current Balance: " + account.getBalance());
        System.out.println("------------------------------------------");
    }

    /** Writes a full formatted statement to the given file path. */
    public void exportFullStatement(String accountNumber, String filePath) throws AccountNotFoundException {
        Account account = requireAccount(accountNumber);

        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("================ ACCOUNT STATEMENT ================");
            writer.println("Account Number : " + account.getAccountNumber());
            writer.println("Account Type   : " + account.getAccountType());
            writer.println("Customer ID    : " + account.getCustomerId());
            writer.println("Opened On      : " + account.getCreatedAt().format(HEADER_FORMAT));
            writer.println("Generated On   : " + java.time.LocalDateTime.now().format(HEADER_FORMAT));
            writer.println("====================================================");
            writer.println();
            writer.printf("%-19s | %-13s | %10s | %-18s | %s%n",
                    "Timestamp", "Type", "Amount", "Balance After", "Description");
            writer.println("----------------------------------------------------------------------------");

            for (Transaction t : account.getTransactions()) {
                writer.println(t.toStatementLine());
            }

            writer.println("----------------------------------------------------------------------------");
            writer.println("Closing Balance: " + account.getBalance());
            FileLogger.info("Exported full statement for " + accountNumber + " to " + filePath);
            System.out.println("Statement exported to: " + filePath);
        } catch (IOException e) {
            FileLogger.error("Failed to export statement for " + accountNumber, e);
            System.err.println("Could not write statement file: " + e.getMessage());
        }
    }
}
