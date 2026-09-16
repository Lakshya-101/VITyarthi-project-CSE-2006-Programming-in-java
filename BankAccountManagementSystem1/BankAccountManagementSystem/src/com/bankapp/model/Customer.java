package com.bankapp.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Represents a bank customer who may own one or more accounts. */
public class Customer implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String customerId;
    private final String name;
    private final String email;
    private final String phone;
    private final List<String> accountNumbers;

    public Customer(String customerId, String name, String email, String phone) {
        this.customerId = customerId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.accountNumbers = new ArrayList<>();
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public List<String> getAccountNumbers() {
        return accountNumbers;
    }

    public void linkAccount(String accountNumber) {
        accountNumbers.add(accountNumber);
    }

    @Override
    public String toString() {
        return String.format("Customer[id=%s, name=%s, email=%s, accounts=%s]",
                customerId, name, email, accountNumbers);
    }
}
