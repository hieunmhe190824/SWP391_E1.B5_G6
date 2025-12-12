package com.carrental.model;

import java.math.BigDecimal;
import java.util.Map;

/**
 * DTO for Dashboard Overview Statistics
 * Combines key metrics from all reports
 */
public class DashboardStatsDTO {
    // Revenue metrics
    private BigDecimal totalRevenue;
    private BigDecimal monthlyRevenue;
    private BigDecimal weeklyRevenue;
    
    // Booking metrics
    private Long totalBookings;
    private Long pendingBookings;
    private Long activeRentals;
    private Long completedRentals;
    
    // Vehicle metrics
    private Long totalVehicles;
    private Long availableVehicles;
    private Long rentedVehicles;
    private Long maintenanceVehicles;
    
    // Customer metrics
    private Long totalCustomers;
    private Long newCustomersThisMonth;
    private Long activeCustomers;
    
    // Contract metrics
    private Long totalContracts;
    private Long activeContracts;
    private Long completedContracts;
    
    // Payment metrics
    private Long pendingPayments;
    private BigDecimal pendingPaymentAmount;
    
    // Chart data
    private Map<String, Long> bookingsByStatus;
    private Map<String, Long> vehiclesByStatus;
    private Map<String, BigDecimal> revenueByMonth;

    public DashboardStatsDTO() {
        this.totalRevenue = BigDecimal.ZERO;
        this.monthlyRevenue = BigDecimal.ZERO;
        this.weeklyRevenue = BigDecimal.ZERO;
        this.totalBookings = 0L;
        this.pendingBookings = 0L;
        this.activeRentals = 0L;
        this.completedRentals = 0L;
        this.totalVehicles = 0L;
        this.availableVehicles = 0L;
        this.rentedVehicles = 0L;
        this.maintenanceVehicles = 0L;
        this.totalCustomers = 0L;
        this.newCustomersThisMonth = 0L;
        this.activeCustomers = 0L;
        this.totalContracts = 0L;
        this.activeContracts = 0L;
        this.completedContracts = 0L;
        this.pendingPayments = 0L;
        this.pendingPaymentAmount = BigDecimal.ZERO;
    }

    // Getters and Setters
    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public BigDecimal getMonthlyRevenue() {
        return monthlyRevenue;
    }

    public void setMonthlyRevenue(BigDecimal monthlyRevenue) {
        this.monthlyRevenue = monthlyRevenue;
    }

    public BigDecimal getWeeklyRevenue() {
        return weeklyRevenue;
    }

    public void setWeeklyRevenue(BigDecimal weeklyRevenue) {
        this.weeklyRevenue = weeklyRevenue;
    }

    public Long getTotalBookings() {
        return totalBookings;
    }

    public void setTotalBookings(Long totalBookings) {
        this.totalBookings = totalBookings;
    }

    public Long getPendingBookings() {
        return pendingBookings;
    }

    public void setPendingBookings(Long pendingBookings) {
        this.pendingBookings = pendingBookings;
    }

    public Long getActiveRentals() {
        return activeRentals;
    }

    public void setActiveRentals(Long activeRentals) {
        this.activeRentals = activeRentals;
    }

    public Long getCompletedRentals() {
        return completedRentals;
    }

    public void setCompletedRentals(Long completedRentals) {
        this.completedRentals = completedRentals;
    }

    public Long getTotalVehicles() {
        return totalVehicles;
    }

    public void setTotalVehicles(Long totalVehicles) {
        this.totalVehicles = totalVehicles;
    }

    public Long getAvailableVehicles() {
        return availableVehicles;
    }

    public void setAvailableVehicles(Long availableVehicles) {
        this.availableVehicles = availableVehicles;
    }

    public Long getRentedVehicles() {
        return rentedVehicles;
    }

    public void setRentedVehicles(Long rentedVehicles) {
        this.rentedVehicles = rentedVehicles;
    }

    public Long getMaintenanceVehicles() {
        return maintenanceVehicles;
    }

    public void setMaintenanceVehicles(Long maintenanceVehicles) {
        this.maintenanceVehicles = maintenanceVehicles;
    }

    public Long getTotalCustomers() {
        return totalCustomers;
    }

    public void setTotalCustomers(Long totalCustomers) {
        this.totalCustomers = totalCustomers;
    }

    public Long getNewCustomersThisMonth() {
        return newCustomersThisMonth;
    }

    public void setNewCustomersThisMonth(Long newCustomersThisMonth) {
        this.newCustomersThisMonth = newCustomersThisMonth;
    }

    public Long getActiveCustomers() {
        return activeCustomers;
    }

    public void setActiveCustomers(Long activeCustomers) {
        this.activeCustomers = activeCustomers;
    }

    public Long getTotalContracts() {
        return totalContracts;
    }

    public void setTotalContracts(Long totalContracts) {
        this.totalContracts = totalContracts;
    }

    public Long getActiveContracts() {
        return activeContracts;
    }

    public void setActiveContracts(Long activeContracts) {
        this.activeContracts = activeContracts;
    }

    public Long getCompletedContracts() {
        return completedContracts;
    }

    public void setCompletedContracts(Long completedContracts) {
        this.completedContracts = completedContracts;
    }

    public Long getPendingPayments() {
        return pendingPayments;
    }

    public void setPendingPayments(Long pendingPayments) {
        this.pendingPayments = pendingPayments;
    }

    public BigDecimal getPendingPaymentAmount() {
        return pendingPaymentAmount;
    }

    public void setPendingPaymentAmount(BigDecimal pendingPaymentAmount) {
        this.pendingPaymentAmount = pendingPaymentAmount;
    }

    public Map<String, Long> getBookingsByStatus() {
        return bookingsByStatus;
    }

    public void setBookingsByStatus(Map<String, Long> bookingsByStatus) {
        this.bookingsByStatus = bookingsByStatus;
    }

    public Map<String, Long> getVehiclesByStatus() {
        return vehiclesByStatus;
    }

    public void setVehiclesByStatus(Map<String, Long> vehiclesByStatus) {
        this.vehiclesByStatus = vehiclesByStatus;
    }

    public Map<String, BigDecimal> getRevenueByMonth() {
        return revenueByMonth;
    }

    public void setRevenueByMonth(Map<String, BigDecimal> revenueByMonth) {
        this.revenueByMonth = revenueByMonth;
    }
}
