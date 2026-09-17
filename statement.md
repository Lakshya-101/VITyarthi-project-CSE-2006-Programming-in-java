# Project Statement

## Problem Statement

Manual or spreadsheet-based tracking of customer accounts and transactions is error-prone,
insecure (credentials often stored or shared in plain text), and offers no real way to
detect suspicious activity or provide customers with an auditable transaction history.
Small-scale banking simulations built purely for demonstration purposes often skip these
concerns entirely, focusing only on basic CRUD operations. This project builds a Bank
Account Management System that goes a step further: it applies core Java concepts to model
a system that handles money movement safely, authenticates users securely, flags unusual
activity, and produces proper statements — while remaining simple enough to run and function
as a standalone console application.

## Scope of the Project

The project is a **single-process, console-based Java application** covering the full
lifecycle of a bank account:

- Customer registration and account opening (SAVINGS / CURRENT)
- PIN-based authentication with lockout protection
- Core transactions: deposit, withdrawal, and transfer between accounts
- A simulated month-end interest posting job, run concurrently across accounts
- A simplified, rule-based fraud-detection simulation (not a production fraud engine)
- Statement generation, both as an on-screen summary and an exported text file
- File-based persistence between runs (no external database required)

**Out of scope:** real-world payment network integration, multi-branch/multi-currency
support, a web or mobile front-end, and connection to any real financial institution or
live data source. These are noted as future enhancements rather than delivered features.

## Target Users

- **Bank customers** — individuals who register, open an account, and perform day-to-day
  transactions (deposits, withdrawals, transfers, viewing statements). This includes students, who are commonly among a bank's first-time account holders and benefit from a simple, low-friction onboarding and transaction flow like the one this project provides.
- **Bank administrators / operations staff** — represented in this project by the two
  admin-only console options: running the month-end interest job and running a fraud scan
  across all accounts.

## High-Level Features

1. **Secure Registration & Login** — salted/hashed PIN storage, account lockout after
   repeated failed logins.
2. **Transaction Engine** — thread-safe deposit, withdrawal, and transfer operations with
   proper validation and exception handling.
3. **Concurrent Interest Posting** — a simulated batch job that credits interest to every
   eligible savings account in parallel using a thread pool.
4. **Fraud-Pattern Simulation** — flags large withdrawals and rapid successive transactions
   as a simplified stand-in for real fraud-detection systems.
5. **Statements & Reporting** — mini-statements on screen and full statements exported to
   file, giving customers an auditable transaction history.
6. **Persistence & Logging** — account/customer data persists across runs via file-based
   storage, and every significant event is written to an audit log file.
