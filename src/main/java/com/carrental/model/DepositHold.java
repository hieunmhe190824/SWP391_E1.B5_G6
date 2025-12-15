package com.carrental.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "deposit_holds")
public class DepositHold {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hold_id")
    private Long id;

    @OneToOne
    @JoinColumn(name = "contract_id", nullable = false, unique = true)
    private Contract contract;

    @Column(name = "deposit_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal depositAmount;

    @Column(name = "deducted_at_return", precision = 18, scale = 2)
    private BigDecimal deductedAtReturn = BigDecimal.ZERO;

    @Column(name = "hold_start_date", nullable = false)
    private LocalDateTime holdStartDate;

    @Column(name = "hold_end_date", nullable = false)
    private LocalDateTime holdEndDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DepositStatus status;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum DepositStatus {
        HOLDING, READY, REFUNDED
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Contract getContract() {
        return contract;
    }

    public void setContract(Contract contract) {
        this.contract = contract;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public void setDepositAmount(BigDecimal depositAmount) {
        this.depositAmount = depositAmount;
    }

    public BigDecimal getDeductedAtReturn() {
        return deductedAtReturn;
    }

    public void setDeductedAtReturn(BigDecimal deductedAtReturn) {
        this.deductedAtReturn = deductedAtReturn;
    }

    public LocalDateTime getHoldStartDate() {
        return holdStartDate;
    }

    public void setHoldStartDate(LocalDateTime holdStartDate) {
        this.holdStartDate = holdStartDate;
    }

    public LocalDateTime getHoldEndDate() {
        return holdEndDate;
    }

    public void setHoldEndDate(LocalDateTime holdEndDate) {
        this.holdEndDate = holdEndDate;
    }

    public DepositStatus getStatus() {
        return status;
    }

    public void setStatus(DepositStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
