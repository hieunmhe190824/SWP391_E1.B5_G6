package com.carrental.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @Convert(converter = PaymentTypeConverter.class)
    @Column(name = "payment_type", nullable = false)
    private PaymentType paymentType;

    @Column(nullable = false)
    private BigDecimal amount;

    @Convert(converter = PaymentMethodConverter.class)
    @Column(name = "method", nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    // Online payment gateway fields
    @Column(name = "transaction_ref", unique = true, length = 50)
    private String transactionRef;

    @Column(name = "gateway_transaction_id", length = 50)
    private String gatewayTransactionId;

    @Column(name = "gateway_response_code", length = 10)
    private String gatewayResponseCode;

    @Column(name = "gateway_transaction_status", length = 10)
    private String gatewayTransactionStatus;

    @Column(name = "gateway_bank_code", length = 20)
    private String gatewayBankCode;

    @Column(name = "gateway_card_type", length = 20)
    private String gatewayCardType;

    @Column(name = "gateway_pay_date", length = 14)
    private String gatewayPayDate;

    @Column(name = "gateway_secure_hash", length = 255)
    private String gatewaySecureHash;

    @Column(name = "payment_url", columnDefinition = "TEXT")
    private String paymentUrl;

    // Bill details (for RENTAL payment type)
    @Column(name = "bill_number", unique = true, length = 50)
    private String billNumber;

    @Column(name = "original_rental_fee")
    private BigDecimal originalRentalFee;

    @Column(name = "rental_adjustment")
    private BigDecimal rentalAdjustment; // Can be negative (early return) or positive (late return)

    @Column(name = "actual_rental_fee")
    private BigDecimal actualRentalFee;

    @Column(name = "late_fee")
    private BigDecimal lateFee;

    @Column(name = "damage_fee")
    private BigDecimal damageFee;

    @Column(name = "one_way_fee")
    private BigDecimal oneWayFee;

    @Column(name = "total_additional_fees")
    private BigDecimal totalAdditionalFees; // Sum of late_fee + damage_fee + one_way_fee

    @Column(name = "deposit_amount")
    private BigDecimal depositAmount; // 50,000,000 VND - still held (for RENTAL payment)

    @Column(name = "amount_paid")
    private BigDecimal amountPaid; // Initially 0, updated when customer pays

    @Column(name = "amount_due")
    private BigDecimal amountDue; // total_amount - amount_paid

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public enum PaymentType {
        DEPOSIT, RENTAL, REFUND
    }

    public enum PaymentMethod {
        CASH, CARD, TRANSFER, ONLINE  // Added ONLINE for payment gateway
    }

    public enum PaymentStatus {
        PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED  // Added PROCESSING and CANCELLED
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

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(PaymentType paymentType) {
        this.paymentType = paymentType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public void setTransactionRef(String transactionRef) {
        this.transactionRef = transactionRef;
    }

    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }

    public void setGatewayTransactionId(String gatewayTransactionId) {
        this.gatewayTransactionId = gatewayTransactionId;
    }

    public String getGatewayResponseCode() {
        return gatewayResponseCode;
    }

    public void setGatewayResponseCode(String gatewayResponseCode) {
        this.gatewayResponseCode = gatewayResponseCode;
    }

    public String getGatewayTransactionStatus() {
        return gatewayTransactionStatus;
    }

    public void setGatewayTransactionStatus(String gatewayTransactionStatus) {
        this.gatewayTransactionStatus = gatewayTransactionStatus;
    }

    public String getGatewayBankCode() {
        return gatewayBankCode;
    }

    public void setGatewayBankCode(String gatewayBankCode) {
        this.gatewayBankCode = gatewayBankCode;
    }

    public String getGatewayCardType() {
        return gatewayCardType;
    }

    public void setGatewayCardType(String gatewayCardType) {
        this.gatewayCardType = gatewayCardType;
    }

    public String getGatewayPayDate() {
        return gatewayPayDate;
    }

    public void setGatewayPayDate(String gatewayPayDate) {
        this.gatewayPayDate = gatewayPayDate;
    }

    public String getGatewaySecureHash() {
        return gatewaySecureHash;
    }

    public void setGatewaySecureHash(String gatewaySecureHash) {
        this.gatewaySecureHash = gatewaySecureHash;
    }

    public String getPaymentUrl() {
        return paymentUrl;
    }

    public void setPaymentUrl(String paymentUrl) {
        this.paymentUrl = paymentUrl;
    }

    // Bill details getters and setters
    public String getBillNumber() {
        return billNumber;
    }

    public void setBillNumber(String billNumber) {
        this.billNumber = billNumber;
    }

    public BigDecimal getOriginalRentalFee() {
        return originalRentalFee;
    }

    public void setOriginalRentalFee(BigDecimal originalRentalFee) {
        this.originalRentalFee = originalRentalFee;
    }

    public BigDecimal getRentalAdjustment() {
        return rentalAdjustment;
    }

    public void setRentalAdjustment(BigDecimal rentalAdjustment) {
        this.rentalAdjustment = rentalAdjustment;
    }

    public BigDecimal getActualRentalFee() {
        return actualRentalFee;
    }

    public void setActualRentalFee(BigDecimal actualRentalFee) {
        this.actualRentalFee = actualRentalFee;
    }

    public BigDecimal getLateFee() {
        return lateFee;
    }

    public void setLateFee(BigDecimal lateFee) {
        this.lateFee = lateFee;
    }

    public BigDecimal getDamageFee() {
        return damageFee;
    }

    public void setDamageFee(BigDecimal damageFee) {
        this.damageFee = damageFee;
    }

    public BigDecimal getOneWayFee() {
        return oneWayFee;
    }

    public void setOneWayFee(BigDecimal oneWayFee) {
        this.oneWayFee = oneWayFee;
    }

    public BigDecimal getTotalAdditionalFees() {
        return totalAdditionalFees;
    }

    public void setTotalAdditionalFees(BigDecimal totalAdditionalFees) {
        this.totalAdditionalFees = totalAdditionalFees;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public void setDepositAmount(BigDecimal depositAmount) {
        this.depositAmount = depositAmount;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    public BigDecimal getAmountDue() {
        return amountDue;
    }

    public void setAmountDue(BigDecimal amountDue) {
        this.amountDue = amountDue;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
