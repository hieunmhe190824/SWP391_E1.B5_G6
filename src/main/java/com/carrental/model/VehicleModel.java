package com.carrental.model;

import jakarta.persistence.*;

@Entity
@Table(name = "vehicle_models")
public class VehicleModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "model_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "brand_id", nullable = false)
    private VehicleBrand brand;

    @Column(name = "model_name", nullable = false)
    private String modelName;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "seats", nullable = false)
    private Integer seats;

    @Column(name = "transmission", nullable = false)
    private String transmission;

    @Column(name = "fuel_type", nullable = false)
    private String fuelType;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public VehicleBrand getBrand() {
        return brand;
    }

    public void setBrand(VehicleBrand brand) {
        this.brand = brand;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getSeats() {
        return seats;
    }

    public void setSeats(Integer seats) {
        this.seats = seats;
    }

    public String getTransmission() {
        return transmission;
    }

    public void setTransmission(String transmission) {
        this.transmission = transmission;
    }

    public String getFuelType() {
        return fuelType;
    }

    public void setFuelType(String fuelType) {
        this.fuelType = fuelType;
    }
}
