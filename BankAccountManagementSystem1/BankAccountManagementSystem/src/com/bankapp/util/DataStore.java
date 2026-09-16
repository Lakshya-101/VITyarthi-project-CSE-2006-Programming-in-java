package com.bankapp.util;

import com.bankapp.model.Account;
import com.bankapp.model.Customer;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Persists the in-memory bank state (customers + accounts) to a local file
 * between application runs, using Java object serialization.
 *
 * This keeps the project dependency-free (no external DB driver needed)
 * while still satisfying the "Storage Design" expectation. The storage
 * format is documented as flat objects rather than a relational schema
 * because this is a single-process console application; swapping this
 * class out for a JDBC-backed implementation is listed as a Future
 * Enhancement in the project report.
 */
public final class DataStore {

    private final String filePath;

    public DataStore(String filePath) {
        this.filePath = filePath;
    }

    /** Bundles both maps into a single serializable snapshot. */
    private static class Snapshot implements Serializable {
        private static final long serialVersionUID = 1L;
        Map<String, Customer> customers;
        Map<String, Account> accounts;

        Snapshot(Map<String, Customer> customers, Map<String, Account> accounts) {
            this.customers = customers;
            this.accounts = accounts;
        }
    }

    public synchronized void save(Map<String, Customer> customers, Map<String, Account> accounts) {
        try (ObjectOutputStream out =
                     new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(filePath)))) {
            out.writeObject(new Snapshot(customers, accounts));
            FileLogger.info("Bank state saved to " + filePath);
        } catch (IOException e) {
            FileLogger.error("Failed to save bank state", e);
            System.err.println("Warning: could not save data — " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public synchronized boolean load(Map<String, Customer> customersOut, Map<String, Account> accountsOut) {
        File file = new File(filePath);
        if (!file.exists()) {
            return false;
        }
        try (ObjectInputStream in =
                     new ObjectInputStream(new BufferedInputStream(new FileInputStream(filePath)))) {
            Snapshot snapshot = (Snapshot) in.readObject();
            customersOut.putAll(snapshot.customers);
            accountsOut.putAll(snapshot.accounts);
            FileLogger.info("Bank state loaded from " + filePath);
            return true;
        } catch (IOException | ClassNotFoundException e) {
            FileLogger.error("Failed to load bank state", e);
            System.err.println("Warning: could not load existing data — starting fresh.");
            return false;
        }
    }
}
