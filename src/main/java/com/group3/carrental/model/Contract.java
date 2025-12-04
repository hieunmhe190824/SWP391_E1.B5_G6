package com.group3.carrental.model;

import java.io.Serializable;
import java.util.Date;

public class Contract implements Serializable {
    private int id;
    private int userId;
    private int carId;
    private Date startDate;
    private Date endDate;
    private String status; // pending, active, cancelled, completed
    private double totalPrice;

    public Contract() {}

    public Contract(int id, int userId, int carId, Date startDate,
                    Date endDate, String status, double totalPrice) {
        this.id = id;
        this.userId = userId;
        this.carId = carId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.totalPrice = totalPrice;
    }

    // getters - setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getCarId() { return carId; }
    public void setCarId(int carId) { this.carId = carId; }

    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }
}
