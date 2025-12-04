package com.carrental.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "refunds")
public class Refund {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refund_id")
    private Long id;

    @OneToOne
    @JoinColumn(name = "hold_id", nullable = false, unique = true)
    private DepositHold depositHold;

    @ManyToOne
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @Column(name = "original_deposit", nullable = false)
    private BigDecimal originalDeposit;

    @Column(name = "deducted_at_return")
    private BigDecimal deductedAtReturn;

    @Column(name = "traffic_fines")
    private BigDecimal trafficFines;

    @Column(name = "refund_amount", nullable = false)
    private BigDecimal refundAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_method", nullable = false)
    private RefundMethod refundMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundStatus status;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum RefundMethod {
        TRANSFER, CASH
    }

    public enum RefundStatus {
        PENDING, COMPLETED
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DepositHold getDepositHold() {
        return depositHold;
    }

    public void setDepositHold(DepositHold depositHold) {
        this.depositHold = depositHold;
    }

    public Contract getContract() {
        return contract;
    }

    public void setContract(Contract contract) {
        this.contract = contract;
    }

    public User getCustomer() {
        return customer;
    }

    public void setCustomer(User customer) {
        this.customer = customer;
    }

    public BigDecimal getOriginalDeposit() {
        return originalDeposit;
    }

    public void setOriginalDeposit(BigDecimal originalDeposit) {
        this.originalDeposit = originalDeposit;
    }

    public BigDecimal getDeductedAtReturn() {
        return deductedAtReturn;
    }

    public void setDeductedAtReturn(BigDecimal deductedAtReturn) {
        this.deductedAtReturn = deductedAtReturn;
    }

    public BigDecimal getTrafficFines() {
        return trafficFines;
    }

    public void setTrafficFines(BigDecimal trafficFines) {
        this.trafficFines = trafficFines;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public RefundMethod getRefundMethod() {
        return refundMethod;
    }

    public void setRefundMethod(RefundMethod refundMethod) {
        this.refundMethod = refundMethod;
    }

    public RefundStatus getStatus() {
        return status;
    }

    public void setStatus(RefundStatus status) {
        this.status = status;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
