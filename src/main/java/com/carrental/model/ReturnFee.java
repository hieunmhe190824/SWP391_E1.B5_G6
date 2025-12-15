package com.carrental.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "return_fees")
public class ReturnFee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fee_id")
    private Long id;

    @OneToOne
    @JoinColumn(name = "contract_id", nullable = false, unique = true)
    private Contract contract;

    @ManyToOne
    @JoinColumn(name = "handover_id", nullable = false)
    private Handover handover;

    @Column(name = "is_late", nullable = false)
    private Boolean isLate;

    @Column(name = "hours_late", precision = 18, scale = 2)
    private BigDecimal hoursLate;

    @Column(name = "late_fee", precision = 18, scale = 2)
    private BigDecimal lateFee;

    @Column(name = "has_damage")
    private Boolean hasDamage;

    @Column(name = "damage_description")
    private String damageDescription;

    @Column(name = "damage_fee", precision = 18, scale = 2)
    private BigDecimal damageFee;

    @Column(name = "is_different_location")
    private Boolean isDifferentLocation;

    @Column(name = "one_way_fee", precision = 18, scale = 2)
    private BigDecimal oneWayFee;

    @Column(name = "total_fees", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalFees;

    @Column(name = "created_at", insertable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

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

    public Handover getHandover() {
        return handover;
    }

    public void setHandover(Handover handover) {
        this.handover = handover;
    }

    public Boolean getLate() {
        return isLate;
    }

    public void setLate(Boolean late) {
        isLate = late;
    }

    public BigDecimal getHoursLate() {
        return hoursLate;
    }

    public void setHoursLate(BigDecimal hoursLate) {
        this.hoursLate = hoursLate;
    }

    public BigDecimal getLateFee() {
        return lateFee;
    }

    public void setLateFee(BigDecimal lateFee) {
        this.lateFee = lateFee;
    }

    public Boolean getHasDamage() {
        return hasDamage;
    }

    public void setHasDamage(Boolean hasDamage) {
        this.hasDamage = hasDamage;
    }

    public String getDamageDescription() {
        return damageDescription;
    }

    public void setDamageDescription(String damageDescription) {
        this.damageDescription = damageDescription;
    }

    public BigDecimal getDamageFee() {
        return damageFee;
    }

    public void setDamageFee(BigDecimal damageFee) {
        this.damageFee = damageFee;
    }

    public Boolean getDifferentLocation() {
        return isDifferentLocation;
    }

    public void setDifferentLocation(Boolean differentLocation) {
        isDifferentLocation = differentLocation;
    }

    public BigDecimal getOneWayFee() {
        return oneWayFee;
    }

    public void setOneWayFee(BigDecimal oneWayFee) {
        this.oneWayFee = oneWayFee;
    }

    public BigDecimal getTotalFees() {
        return totalFees;
    }

    public void setTotalFees(BigDecimal totalFees) {
        this.totalFees = totalFees;
    }

    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
