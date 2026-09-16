# Bank Account Management System

A console-based Bank Account Management System built in core Java, developed as a
"Build Your Own Project" submission for the **Programming in Java** course.

## Overview

The system lets customers register, open savings/current accounts, and perform day-to-day
banking operations (deposit, withdraw, transfer) through a menu-driven console interface.
It layers in the concerns a real banking system has to deal with — security (hashed PINs),
concurrency safety on shared account balances, basic fraud-pattern detection, statement
generation, and audit logging — while staying dependency-free so it can be compiled and run
with nothing but a plain JDK.

## Features

### Module 1 — Registration & Authentication
- Customer registration and account opening (SAVINGS or CURRENT)
- PIN-based login, with PINs salted and hashed (SHA-256) — never stored or compared in plain text
- Automatic account lockout (FROZEN status) after 3 consecutive failed login attempts
- Change-PIN functionality

### Module 2 — Transactions
- Deposit, withdraw, and transfer between accounts
- All balance-mutating operations are `synchronized` at the `Account` level for thread safety
- Transfers acquire locks on both accounts in a fixed order to avoid deadlocks
- Month-end interest posting job that runs **concurrently across all accounts** using a
  fixed thread pool (`ExecutorService`), simulating a scalable batch job

### Module 3 — Fraud Simulation & Statements
- Rule-based fraud-check simulation: flags unusually large withdrawals and rapid-fire
  ("velocity") transaction patterns, logging alerts without hard-blocking the demo flow
- Admin-triggered fraud scan across all accounts
- On-screen mini-statement (last N transactions)
- Full statement export to a formatted `.txt` file on disk

## Non-Functional Requirements Addressed

| Requirement       | How it's addressed                                                          |
|--------------------|------------------------------------------------------------------------------|
| **Security**        | Salted SHA-256 PIN hashing, constant-time hash comparison, account lockout   |
| **Reliability**     | Centralized input validation, checked-exception hierarchy, no silent failures |
| **Performance**      | `ConcurrentHashMap` for O(1) account/customer lookups                        |
| **Scalability**      | Multithreaded interest-posting batch job via `ExecutorService`               |
| **Maintainability**  | Layered architecture: `model` / `service` / `util` / `exception` packages    |
| **Logging & Monitoring** | Every login, transaction, and fraud alert is written to `bank_app.log`    |
| **Resource efficiency**  | Try-with-resources for all file I/O, executor shutdown on job completion |

## Technologies / Tools Used

- **Java (JDK 17+)** — core language, no external libraries
- `java.util.concurrent` — `ExecutorService`, `ConcurrentHashMap` for thread safety & scalability
- `java.security.MessageDigest` — SHA-256 PIN hashing
- `java.io` / Java Serialization — flat-file persistence (`bank_data.dat`)
- `java.util.logging` — file-based audit logging (`bank_app.log`)
- Plain assertion-based test harness (no external test framework required)

## Project Structure

```
BankAccountManagementSystem/
├── README.md
├── statement.md
└── src/
    └── com/bankapp/
        ├── Main.java                       # Console entry point / menu
        ├── model/
        │   ├── Account.java                # Thread-safe account with balance ops
        │   ├── Customer.java
        │   ├── Transaction.java
        │   └── TransactionType.java
        ├── service/
        │   ├── AuthService.java             # Module 1: registration & login
        │   ├── AccountService.java          # Module 2: deposit/withdraw/transfer/interest
        │   ├── FraudDetectionService.java   # Module 3a: fraud-check simulation
        │   ├── StatementService.java        # Module 3b: statements & reporting
        │   └── BankRepository.java          # Shared in-memory data store
        ├── exception/
        │   ├── BankException.java           # Base checked exception
        │   ├── AccountNotFoundException.java
        │   ├── AuthenticationException.java
        │   ├── DuplicateAccountException.java
        │   ├── InsufficientFundsException.java
        │   └── InvalidAmountException.java
        ├── util/
        │   ├── PasswordUtil.java            # Salted SHA-256 hashing
        │   ├── Validator.java               # Centralized input validation
        │   ├── FileLogger.java              # Audit logging wrapper
        │   └── DataStore.java               # File-based persistence
        └── test/
            └── BankServiceTest.java         # Assertion-based test suite
```

## Steps to Install & Run

Requires only a JDK (17 or newer) — no build tool or external dependency needed.
Check with `java -version` and `javac -version`; if `javac` isn't found, install a
JDK (e.g. from https://adoptium.net, or `sudo apt install openjdk-21-jdk` on Ubuntu/Debian,
`brew install openjdk` on Mac).

**1. Clone the repository**
```bash
git clone <your-repo-url>
cd BankAccountManagementSystem
```

**2. Compile all source files into an `out` directory**

macOS / Linux (bash):
```bash
javac -d out $(find src -name "*.java")
```

Windows (PowerShell):
```powershell
$files = Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName }
javac -d out $files
```

**3. Run the application**
```bash
java -cp out com.bankapp.Main
```

This works the same on every platform once compiled.

**Re-running later:** if you haven't changed any `.java` files, you don't need to
recompile — just re-run step 3. Only recompile (step 2) after editing, adding, or
removing source files. If you ever delete a source file, delete the `out` folder
and recompile fresh so no stale `.class` files linger:
```bash
# macOS/Linux
rm -rf out
# Windows PowerShell
Remove-Item -Recurse -Force out
```

On first run the app creates `bank_data.dat` (persisted state) and `bank_app.log`
(audit log) in the working directory. On subsequent runs it automatically reloads
your data, so accounts persist across sessions — just make sure you always run it
from the same folder.

Alternatively, if you're using an IDE (IntelliJ, Eclipse, VS Code with the Java
extension), just open the project folder, mark `src` as the source root, and run
`Main.java` directly — no terminal commands needed.

## Instructions for Testing

A lightweight, dependency-free test harness using Java's built-in `assert` statements
covers account creation, deposits/withdrawals, insufficient-funds handling, PIN
authentication (success and failure), invalid-amount rejection, and transfers.

```bash
java -ea -cp out com.bankapp.test.BankServiceTest
```
(This works the same in PowerShell too — no file-list needed since it's just one class.)

`-ea` enables assertions. The harness prints `[PASS]` / `[FAIL]` per test case and a
final summary, exiting with a non-zero status if any test fails.

## Sample Usage Walkthrough

1. Choose **1** to register a new customer and open an account — note the generated
   account number.
2. Choose **2** to log in with that account number and your PIN.
3. From the session menu, deposit/withdraw/transfer funds, view your balance, or
   generate a mini/full statement.
4. From the main menu, options **3** and **4** are admin-only utilities: posting
   monthly interest across all savings accounts, and running a fraud scan.
5. Choose **0** to log out or exit — your data is saved automatically.

## Future Enhancements

- Replace flat-file persistence with JDBC-backed storage (MySQL/PostgreSQL)
- Add a JavaFX/Swing GUI in place of the console interface
- Migrate the test harness to JUnit 5 once a build tool (Maven/Gradle) is introduced
- Externalize fraud-detection thresholds into a configuration file
