package com.carrental.model;

import java.math.BigDecimal;

/**
 * DTO for Customer Statistics
 * Contains customer activity and revenue metrics
 */
public class CustomerStatsDTO {
    private Long customerId;
    private String fullName;
    private String email;
    private String phone;
    
    // Statistics
    private Long totalBookings;
    private Long completedBookings;
    private Long cancelledBookings;
    private BigDecimal totalSpent;
    
    // Registration info
    private String registrationDate;

    public CustomerStatsDTO() {
    }

    public CustomerStatsDTO(Long customerId, String fullName, String email, String phone,
                           Long totalBookings, Long completedBookings, BigDecimal totalSpent) {
        this.customerId = customerId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.totalBookings = totalBookings;
        this.completedBookings = completedBookings;
        this.totalSpent = totalSpent;
    }

    // Getters and Setters
    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Long getTotalBookings() {
        return totalBookings;
    }

    public void setTotalBookings(Long totalBookings) {
        this.totalBookings = totalBookings;
    }

    public Long getCompletedBookings() {
        return completedBookings;
    }

    public void setCompletedBookings(Long completedBookings) {
        this.completedBookings = completedBookings;
    }

    public Long getCancelledBookings() {
        return cancelledBookings;
    }

    public void setCancelledBookings(Long cancelledBookings) {
        this.cancelledBookings = cancelledBookings;
    }

    public BigDecimal getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(BigDecimal totalSpent) {
        this.totalSpent = totalSpent;
    }

    public String getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(String registrationDate) {
        this.registrationDate = registrationDate;
    }
}
