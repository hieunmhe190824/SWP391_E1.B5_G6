package com.carrental.model;

import java.math.BigDecimal;
import java.util.Map;

/**
 * DTO for Revenue Report
 * Contains revenue statistics by payment type, method, and time period
 */
public class RevenueReportDTO {
    private BigDecimal totalRevenue;
    private BigDecimal depositRevenue;
    private BigDecimal rentalRevenue;
    private BigDecimal refundAmount;
    
    // Revenue by payment method
    private BigDecimal cashRevenue;
    private BigDecimal cardRevenue;
    private BigDecimal transferRevenue;
    private BigDecimal onlineRevenue;
    
    // Statistics
    private Long totalPayments;
    private Long completedPayments;
    private Long pendingPayments;
    
    // Trend data for charts (date -> revenue)
    private Map<String, BigDecimal> dailyRevenue;
    
    public RevenueReportDTO() {
        this.totalRevenue = BigDecimal.ZERO;
        this.depositRevenue = BigDecimal.ZERO;
        this.rentalRevenue = BigDecimal.ZERO;
        this.refundAmount = BigDecimal.ZERO;
        this.cashRevenue = BigDecimal.ZERO;
        this.cardRevenue = BigDecimal.ZERO;
        this.transferRevenue = BigDecimal.ZERO;
        this.onlineRevenue = BigDecimal.ZERO;
        this.totalPayments = 0L;
        this.completedPayments = 0L;
        this.pendingPayments = 0L;
    }

    // Getters and Setters
    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public BigDecimal getDepositRevenue() {
        return depositRevenue;
    }

    public void setDepositRevenue(BigDecimal depositRevenue) {
        this.depositRevenue = depositRevenue;
    }

    public BigDecimal getRentalRevenue() {
        return rentalRevenue;
    }

    public void setRentalRevenue(BigDecimal rentalRevenue) {
        this.rentalRevenue = rentalRevenue;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public BigDecimal getCashRevenue() {
        return cashRevenue;
    }

    public void setCashRevenue(BigDecimal cashRevenue) {
        this.cashRevenue = cashRevenue;
    }

    public BigDecimal getCardRevenue() {
        return cardRevenue;
    }

    public void setCardRevenue(BigDecimal cardRevenue) {
        this.cardRevenue = cardRevenue;
    }

    public BigDecimal getTransferRevenue() {
        return transferRevenue;
    }

    public void setTransferRevenue(BigDecimal transferRevenue) {
        this.transferRevenue = transferRevenue;
    }

    public BigDecimal getOnlineRevenue() {
        return onlineRevenue;
    }

    public void setOnlineRevenue(BigDecimal onlineRevenue) {
        this.onlineRevenue = onlineRevenue;
    }

    public Long getTotalPayments() {
        return totalPayments;
    }

    public void setTotalPayments(Long totalPayments) {
        this.totalPayments = totalPayments;
    }

    public Long getCompletedPayments() {
        return completedPayments;
    }

    public void setCompletedPayments(Long completedPayments) {
        this.completedPayments = completedPayments;
    }

    public Long getPendingPayments() {
        return pendingPayments;
    }

    public void setPendingPayments(Long pendingPayments) {
        this.pendingPayments = pendingPayments;
    }

    public Map<String, BigDecimal> getDailyRevenue() {
        return dailyRevenue;
    }

    public void setDailyRevenue(Map<String, BigDecimal> dailyRevenue) {
        this.dailyRevenue = dailyRevenue;
    }
}
