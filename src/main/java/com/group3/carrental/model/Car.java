package com.group3.carrental.model;

import java.io.Serializable;

public class Car implements Serializable {
    private int id;
    private String licensePlate;
    private String brand;
    private String model;
    private int year;
    private String status; // available, booked, rented, maintenance
    private double pricePerDay;
    private String imageUrl;

    public Car() {}

    public Car(int id, String licensePlate, String brand, String model, int year,
               String status, double pricePerDay, String imageUrl) {
        this.id = id;
        this.licensePlate = licensePlate;
        this.brand = brand;
        this.model = model;
        this.year = year;
        this.status = status;
        this.pricePerDay = pricePerDay;
        this.imageUrl = imageUrl;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getPricePerDay() { return pricePerDay; }
    public void setPricePerDay(double pricePerDay) { this.pricePerDay = pricePerDay; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
