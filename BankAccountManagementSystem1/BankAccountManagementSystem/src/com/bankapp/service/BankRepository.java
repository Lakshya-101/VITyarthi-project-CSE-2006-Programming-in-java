package com.bankapp.service;

import com.bankapp.model.Account;
import com.bankapp.model.Customer;
import com.bankapp.util.DataStore;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Central in-memory store of all customers and accounts.
 * NON-FUNCTIONAL REQUIREMENT: Performance — uses {@link ConcurrentHashMap}
 * for O(1) average-case lookup by account/customer id, and to allow safe
 * concurrent reads/writes from multiple threads (e.g. the interest-posting
 * batch job running alongside live user transactions).
 */
public class BankRepository {

    private final Map<String, Customer> customers = new ConcurrentHashMap<>();
    private final Map<String, Account> accounts = new ConcurrentHashMap<>();
    private final AtomicInteger customerSequence = new AtomicInteger(1000);
    private final AtomicInteger accountSequence = new AtomicInteger(100000);
    private final DataStore dataStore;

    public BankRepository(String storageFilePath) {
        this.dataStore = new DataStore(storageFilePath);
        dataStore.load(customers, accounts);
    }

    public String nextCustomerId() {
        return "CUST" + customerSequence.incrementAndGet();
    }

    public String nextAccountNumber() {
        return "ACC" + accountSequence.incrementAndGet();
    }

    public void addCustomer(Customer customer) {
        customers.put(customer.getCustomerId(), customer);
    }

    public void addAccount(Account account) {
        accounts.put(account.getAccountNumber(), account);
    }

    public Customer getCustomer(String customerId) {
        return customers.get(customerId);
    }

    public Account getAccount(String accountNumber) {
        return accounts.get(accountNumber);
    }

    public Map<String, Customer> getAllCustomers() {
        return customers;
    }

    public Map<String, Account> getAllAccounts() {
        return accounts;
    }

    /** Persists the current state to disk immediately (e.g. on graceful shutdown). */
    public void persist() {
        dataStore.save(customers, accounts);
    }
}
