package com.bankapp;

import com.bankapp.exception.BankException;
import com.bankapp.model.Account;
import com.bankapp.model.Customer;
import com.bankapp.service.AccountService;
import com.bankapp.service.AuthService;
import com.bankapp.service.BankRepository;
import com.bankapp.service.FraudDetectionService;
import com.bankapp.service.StatementService;
import com.bankapp.util.FileLogger;

import java.math.BigDecimal;
import java.util.Scanner;

/**
 * Console entry point for the Bank Account Management System.
 * Wires together all three functional modules:
 *   1. Registration & Authentication (AuthService)
 *   2. Transactions: deposit/withdraw/transfer + interest (AccountService)
 *   3. Fraud simulation + Statements/reporting (FraudDetectionService, StatementService)
 */
public class Main {

    private static final String DATA_FILE = "bank_data.dat";
    private static final String LOG_FILE = "bank_app.log";

    private static final Scanner SCANNER = new Scanner(System.in);
    private static BankRepository repository;
    private static AuthService authService;
    private static AccountService accountService;
    private static StatementService statementService;
    private static FraudDetectionService fraudDetectionService;

    public static void main(String[] args) {
        FileLogger.init(LOG_FILE);
        repository = new BankRepository(DATA_FILE);
        fraudDetectionService = new FraudDetectionService();
        authService = new AuthService(repository);
        accountService = new AccountService(repository, fraudDetectionService);
        statementService = new StatementService(repository);

        // Persist data automatically on Ctrl+C / normal JVM exit too.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> repository.persist()));

        System.out.println("=======================================");
        System.out.println(" Welcome to the Bank Account Management System");
        System.out.println("=======================================");

        boolean running = true;
        while (running) {
            printMainMenu();
            String choice = SCANNER.nextLine().trim();
            switch (choice) {
                case "1": registerCustomerAndAccount(); break;
                case "2": login(); break;
                case "3": runAdminInterestJob(); break;
                case "4": runFraudScan(); break;
                case "0": running = false; break;
                default: System.out.println("Invalid choice. Try again."); break;
            }
        }

        repository.persist();
        System.out.println("Data saved. Goodbye!");
    }

    private static void printMainMenu() {
        System.out.println();
        System.out.println("1. Register new customer + open account");
        System.out.println("2. Login to existing account");
        System.out.println("3. [Admin] Run month-end interest posting job");
        System.out.println("4. [Admin] Run fraud scan across all accounts");
        System.out.println("0. Exit");
        System.out.print("Choose an option: ");
    }

    private static void registerCustomerAndAccount() {
        try {
            System.out.print("Full name: ");
            String name = SCANNER.nextLine().trim();
            System.out.print("Email: ");
            String email = SCANNER.nextLine().trim();
            System.out.print("Phone: ");
            String phone = SCANNER.nextLine().trim();

            Customer customer = authService.registerCustomer(name, email, phone);

            System.out.print("Account type (1=SAVINGS, 2=CURRENT): ");
            String typeChoice = SCANNER.nextLine().trim();
            Account.AccountType type = "2".equals(typeChoice) ? Account.AccountType.CURRENT : Account.AccountType.SAVINGS;

            System.out.print("Opening deposit amount: ");
            BigDecimal openingBalance = new BigDecimal(SCANNER.nextLine().trim());

            System.out.print("Set a 4-6 digit PIN: ");
            String pin = SCANNER.nextLine().trim();

            Account account = authService.openAccount(customer.getCustomerId(), type, openingBalance, pin);
            System.out.println("Success! Your account number is: " + account.getAccountNumber());
            System.out.println("Keep this safe — you'll need it along with your PIN to log in.");
        } catch (BankException e) {
            System.out.println("Registration failed: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid numeric amount.");
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid input: " + e.getMessage());
        }
    }

    private static void login() {
        try {
            System.out.print("Account number: ");
            String accountNumber = SCANNER.nextLine().trim();
            System.out.print("PIN: ");
            String pin = SCANNER.nextLine().trim();

            Account account = authService.login(accountNumber, pin);
            System.out.println("Login successful. Welcome back!");
            sessionMenu(account);
        } catch (BankException e) {
            System.out.println("Login failed: " + e.getMessage());
        }
    }

    private static void sessionMenu(Account account) {
        boolean inSession = true;
        while (inSession) {
            System.out.println();
            System.out.println("--- Account " + account.getAccountNumber() + " ---");
            System.out.println("1. Deposit");
            System.out.println("2. Withdraw");
            System.out.println("3. Transfer");
            System.out.println("4. View balance");
            System.out.println("5. Mini statement (last 5 transactions)");
            System.out.println("6. Export full statement to file");
            System.out.println("7. Change PIN");
            System.out.println("0. Logout");
            System.out.print("Choose an option: ");
            String choice = SCANNER.nextLine().trim();

            try {
                switch (choice) {
                    case "1":
                        System.out.print("Amount to deposit: ");
                        accountService.deposit(account.getAccountNumber(), new BigDecimal(SCANNER.nextLine().trim()));
                        System.out.println("Deposit successful. New balance: " + account.getBalance());
                        break;
                    case "2":
                        System.out.print("Amount to withdraw: ");
                        accountService.withdraw(account.getAccountNumber(), new BigDecimal(SCANNER.nextLine().trim()));
                        System.out.println("Withdrawal successful. New balance: " + account.getBalance());
                        break;
                    case "3":
                        System.out.print("Destination account number: ");
                        String toAccount = SCANNER.nextLine().trim();
                        System.out.print("Amount to transfer: ");
                        BigDecimal amount = new BigDecimal(SCANNER.nextLine().trim());
                        accountService.transfer(account.getAccountNumber(), toAccount, amount);
                        System.out.println("Transfer successful. New balance: " + account.getBalance());
                        break;
                    case "4":
                        System.out.println("Current balance: " + accountService.getBalance(account.getAccountNumber()));
                        break;
                    case "5":
                        statementService.printMiniStatement(account.getAccountNumber(), 5);
                        break;
                    case "6":
                        String fileName = "statement_" + account.getAccountNumber() + ".txt";
                        statementService.exportFullStatement(account.getAccountNumber(), fileName);
                        break;
                    case "7":
                        System.out.print("New PIN (4-6 digits): ");
                        authService.changePin(account, SCANNER.nextLine().trim());
                        System.out.println("PIN updated successfully.");
                        break;
                    case "0":
                        inSession = false;
                        System.out.println("Logged out.");
                        break;
                    default:
                        System.out.println("Invalid choice. Try again.");
                }
            } catch (BankException e) {
                System.out.println("Operation failed: " + e.getMessage());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid numeric amount.");
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid input: " + e.getMessage());
            }
        }
    }

    private static void runAdminInterestJob() {
        System.out.println("Running month-end interest posting job across all savings accounts...");
        int count = accountService.applyMonthlyInterestToAll();
        System.out.println("Interest posted to " + count + " account(s).");
    }

    private static void runFraudScan() {
        fraudDetectionService.scanAllAccounts(repository.getAllAccounts().values());
    }
}
