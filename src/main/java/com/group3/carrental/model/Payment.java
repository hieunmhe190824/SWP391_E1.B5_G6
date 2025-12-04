package com.group3.carrental.model;

import java.io.Serializable;
import java.util.Date;

public class Payment implements Serializable {
    private int id;
    private int contractId;
    private double amount;
    private String method; // vnpay, cash
    private Date paidAt;

    public Payment() {}

    public Payment(int id, int contractId, double amount,
                   String method, Date paidAt) {
        this.id = id;
        this.contractId = contractId;
        this.amount = amount;
        this.method = method;
        this.paidAt = paidAt;
    }

    // getters + setters
}
