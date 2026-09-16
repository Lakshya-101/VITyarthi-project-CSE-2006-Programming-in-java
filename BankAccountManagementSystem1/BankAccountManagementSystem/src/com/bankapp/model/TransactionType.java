package com.bankapp.model;

/** Classifies what a {@link Transaction} represents in an account's ledger. */
public enum TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER_IN,
    TRANSFER_OUT,
    INTEREST_CREDIT
}
