package com.bankapp.test;

import com.bankapp.exception.AuthenticationException;
import com.bankapp.exception.BankException;
import com.bankapp.exception.InsufficientFundsException;
import com.bankapp.exception.InvalidAmountException;
import com.bankapp.model.Account;
import com.bankapp.model.Customer;
import com.bankapp.service.AccountService;
import com.bankapp.service.AuthService;
import com.bankapp.service.BankRepository;
import com.bankapp.service.FraudDetectionService;

import java.math.BigDecimal;
import java.io.File;

/**
 * Lightweight assertion-based test harness.
 *
 * This project intentionally avoids an external test framework (e.g. JUnit)
 * so it can be compiled and run with nothing but a plain JDK — no build
 * tool or downloaded dependency required for evaluation. Run with:
 *   javac -d out $(find src -name "*.java")
 *   java -ea -cp out com.bankapp.test.BankServiceTest
 *
 * The -ea flag enables Java's built-in `assert` statements.
 */
public class BankServiceTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        String testDataFile = "test_bank_data.dat";
        new File(testDataFile).delete(); // ensure a clean slate for the test run

        BankRepository repository = new BankRepository(testDataFile);
        FraudDetectionService fraudService = new FraudDetectionService();
        AuthService authService = new AuthService(repository);
        AccountService accountService = new AccountService(repository, fraudService);

        testRegistrationAndAccountCreation(authService);
        testDepositAndWithdraw(authService, accountService);
        testInsufficientFunds(authService, accountService);
        testWrongPinRejected(authService);
        testInvalidAmountRejected(authService, accountService);
        testTransferBetweenAccounts(authService, accountService);

        new File(testDataFile).delete(); // clean up test artifact

        System.out.println();
        System.out.println("===================================");
        System.out.println("Tests passed: " + passed + " | failed: " + failed);
        System.out.println("===================================");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void check(String testName, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("[PASS] " + testName);
        } else {
            failed++;
            System.out.println("[FAIL] " + testName);
        }
    }

    private static void testRegistrationAndAccountCreation(AuthService authService) throws BankException {
        Customer customer = authService.registerCustomer("Test User", "test@example.com", "9999999999");
        Account account = authService.openAccount(customer.getCustomerId(), Account.AccountType.SAVINGS,
                new BigDecimal("1000.00"), "1234");
        check("Account created with correct opening balance",
                account.getBalance().compareTo(new BigDecimal("1000.00")) == 0);
        check("PIN is hashed, not stored in plain text",
                !account.getPinHash().equals("1234"));
    }

    private static void testDepositAndWithdraw(AuthService authService, AccountService accountService)
            throws BankException {
        Customer customer = authService.registerCustomer("Dep Wit User", "depwit@example.com", "8888888888");
        Account account = authService.openAccount(customer.getCustomerId(), Account.AccountType.SAVINGS,
                new BigDecimal("500.00"), "4321");

        accountService.deposit(account.getAccountNumber(), new BigDecimal("250.00"));
        check("Balance after deposit is correct",
                accountService.getBalance(account.getAccountNumber()).compareTo(new BigDecimal("750.00")) == 0);

        accountService.withdraw(account.getAccountNumber(), new BigDecimal("300.00"));
        check("Balance after withdrawal is correct",
                accountService.getBalance(account.getAccountNumber()).compareTo(new BigDecimal("450.00")) == 0);
    }

    private static void testInsufficientFunds(AuthService authService, AccountService accountService)
            throws BankException {
        Customer customer = authService.registerCustomer("Poor User", "poor@example.com", "7777777777");
        Account account = authService.openAccount(customer.getCustomerId(), Account.AccountType.SAVINGS,
                new BigDecimal("100.00"), "1111");

        boolean threw = false;
        try {
            accountService.withdraw(account.getAccountNumber(), new BigDecimal("999.00"));
        } catch (InsufficientFundsException e) {
            threw = true;
        }
        check("Withdrawing more than balance throws InsufficientFundsException", threw);
    }

    private static void testWrongPinRejected(AuthService authService) throws BankException {
        Customer customer = authService.registerCustomer("Secure User", "secure@example.com", "6666666666");
        Account account = authService.openAccount(customer.getCustomerId(), Account.AccountType.SAVINGS,
                new BigDecimal("100.00"), "5678");

        boolean threw = false;
        try {
            authService.login(account.getAccountNumber(), "0000");
        } catch (AuthenticationException e) {
            threw = true;
        }
        check("Login with wrong PIN throws AuthenticationException", threw);

        Account loggedIn = authService.login(account.getAccountNumber(), "5678");
        check("Login with correct PIN succeeds", loggedIn != null);
    }

    private static void testInvalidAmountRejected(AuthService authService, AccountService accountService)
            throws BankException {
        Customer customer = authService.registerCustomer("Neg User", "neg@example.com", "5555555555");
        Account account = authService.openAccount(customer.getCustomerId(), Account.AccountType.SAVINGS,
                new BigDecimal("100.00"), "2222");

        boolean threw = false;
        try {
            accountService.deposit(account.getAccountNumber(), new BigDecimal("-50.00"));
        } catch (InvalidAmountException e) {
            threw = true;
        }
        check("Depositing a negative amount throws InvalidAmountException", threw);
    }

    private static void testTransferBetweenAccounts(AuthService authService, AccountService accountService)
            throws BankException {
        Customer c1 = authService.registerCustomer("Sender", "sender@example.com", "4444444444");
        Account from = authService.openAccount(c1.getCustomerId(), Account.AccountType.SAVINGS,
                new BigDecimal("500.00"), "1111");

        Customer c2 = authService.registerCustomer("Receiver", "receiver@example.com", "3333333333");
        Account to = authService.openAccount(c2.getCustomerId(), Account.AccountType.SAVINGS,
                new BigDecimal("0.00"), "2222");

        accountService.transfer(from.getAccountNumber(), to.getAccountNumber(), new BigDecimal("200.00"));

        check("Sender balance decreased after transfer",
                accountService.getBalance(from.getAccountNumber()).compareTo(new BigDecimal("300.00")) == 0);
        check("Receiver balance increased after transfer",
                accountService.getBalance(to.getAccountNumber()).compareTo(new BigDecimal("200.00")) == 0);
    }
}
