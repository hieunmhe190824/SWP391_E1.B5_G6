package com.group3.carrental.model;

import java.io.Serializable;
import java.util.Date;

public class Deposit implements Serializable {
    private int id;
    private int contractId;
    private double amount;
    private String status; // paid, refunded
    private Date createdAt;

    public Deposit() {}

    public Deposit(int id, int contractId, double amount,
                   String status, Date createdAt) {
        this.id = id;
        this.contractId = contractId;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
    }

    // getters setters...
}
