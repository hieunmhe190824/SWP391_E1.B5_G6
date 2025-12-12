package com.carrental.model;

import java.math.BigDecimal;

/**
 * DTO for Vehicle Usage Statistics
 * Contains rental count, utilization rate, and revenue per vehicle
 */
public class VehicleUsageDTO {
    private Long vehicleId;
    private String licensePlate;
    private String brandName;
    private String modelName;
    private String category;
    
    // Usage statistics
    private Long rentalCount;
    private Long totalDaysRented;
    private BigDecimal totalRevenue;
    private BigDecimal utilizationRate; // Percentage
    
    // Current status
    private String currentStatus;

    public VehicleUsageDTO() {
    }

    public VehicleUsageDTO(Long vehicleId, String licensePlate, String brandName, String modelName, 
                          String category, Long rentalCount, Long totalDaysRented, BigDecimal totalRevenue) {
        this.vehicleId = vehicleId;
        this.licensePlate = licensePlate;
        this.brandName = brandName;
        this.modelName = modelName;
        this.category = category;
        this.rentalCount = rentalCount;
        this.totalDaysRented = totalDaysRented;
        this.totalRevenue = totalRevenue;
    }

    // Getters and Setters
    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
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

    public Long getRentalCount() {
        return rentalCount;
    }

    public void setRentalCount(Long rentalCount) {
        this.rentalCount = rentalCount;
    }

    public Long getTotalDaysRented() {
        return totalDaysRented;
    }

    public void setTotalDaysRented(Long totalDaysRented) {
        this.totalDaysRented = totalDaysRented;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public BigDecimal getUtilizationRate() {
        return utilizationRate;
    }

    public void setUtilizationRate(BigDecimal utilizationRate) {
        this.utilizationRate = utilizationRate;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    public String getFullName() {
        return brandName + " " + modelName;
    }
}
