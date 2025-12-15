package com.carrental.service;

import com.carrental.model.*;
import com.carrental.model.Refund.RefundMethod;
import com.carrental.model.Refund.RefundStatus;
import com.carrental.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class RefundService {

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private DepositHoldRepository depositHoldRepository;

    @Autowired
    private TrafficViolationRepository violationRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private NotificationService notificationService;

    /**
     * Process deposit refund
     */
    @Transactional
    public Refund processRefund(Long holdId, RefundMethod refundMethod) {
        // Get deposit hold
        DepositHold depositHold = depositHoldRepository.findById(holdId)
                .orElseThrow(() -> new RuntimeException("Deposit hold not found"));

        // Verify status is READY
        if (depositHold.getStatus() != DepositHold.DepositStatus.READY) {
            throw new IllegalStateException("Deposit hold is not ready for refund. Current status: " + depositHold.getStatus());
        }

        // Check if refund already exists
        Optional<Refund> existingRefund = refundRepository.findByDepositHoldId(holdId);
        if (existingRefund.isPresent()) {
            throw new IllegalStateException("Refund already processed for this deposit hold");
        }

        // Calculate refund amount
        BigDecimal originalDeposit = depositHold.getDepositAmount();
        BigDecimal deductedAtReturn = depositHold.getDeductedAtReturn();
        BigDecimal trafficFines = violationRepository.sumFineAmountByDepositHoldId(holdId);
        
        BigDecimal refundAmount = originalDeposit.subtract(deductedAtReturn).subtract(trafficFines);

        // Ensure refund amount is not negative
        if (refundAmount.compareTo(BigDecimal.ZERO) < 0) {
            refundAmount = BigDecimal.ZERO;
        }

        // Create refund record
        Refund refund = new Refund();
        refund.setDepositHold(depositHold);
        refund.setContract(depositHold.getContract());
        refund.setCustomer(depositHold.getContract().getCustomer());
        refund.setOriginalDeposit(originalDeposit);
        refund.setDeductedAtReturn(deductedAtReturn);
        refund.setTrafficFines(trafficFines);
        refund.setRefundAmount(refundAmount);
        refund.setRefundMethod(refundMethod);
        refund.setStatus(RefundStatus.PENDING);
        refund.setProcessedAt(LocalDateTime.now());

        Refund savedRefund = refundRepository.save(refund);

        // Update deposit hold status
        depositHold.setStatus(DepositHold.DepositStatus.REFUNDED);
        depositHoldRepository.save(depositHold);

        // Create refund payment record
        createRefundPayment(depositHold.getContract(), savedRefund, refundAmount, refundMethod);

        // Send notification to customer
        sendRefundNotification(depositHold.getContract().getCustomer(), savedRefund);

        // Mark refund as completed
        savedRefund.setStatus(RefundStatus.COMPLETED);
        return refundRepository.save(savedRefund);
    }

    /**
     * Get refunds by status
     */
    public List<Refund> getRefundsByStatus(RefundStatus status) {
        return refundRepository.findByStatus(status);
    }

    /**
     * Get refund by deposit hold
     */
    public Optional<Refund> getRefundByDepositHold(Long holdId) {
        return refundRepository.findByDepositHoldId(holdId);
    }

    /**
     * Calculate refund amount (preview before processing)
     */
    public BigDecimal calculateRefundAmount(Long holdId) {
        DepositHold depositHold = depositHoldRepository.findById(holdId)
                .orElseThrow(() -> new RuntimeException("Deposit hold not found"));

        BigDecimal originalDeposit = depositHold.getDepositAmount();
        BigDecimal deductedAtReturn = depositHold.getDeductedAtReturn();
        BigDecimal trafficFines = violationRepository.sumFineAmountByDepositHoldId(holdId);
        
        BigDecimal refundAmount = originalDeposit.subtract(deductedAtReturn).subtract(trafficFines);
        
        return refundAmount.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : refundAmount;
    }

    /**
     * Create refund payment record
     */
    private void createRefundPayment(Contract contract, Refund refund, BigDecimal amount, RefundMethod method) {
        Payment payment = new Payment();
        payment.setContract(contract);
        payment.setPaymentType(Payment.PaymentType.REFUND);
        payment.setAmount(amount);
        
        // Map refund method to payment method
        Payment.PaymentMethod paymentMethod;
        if (method == RefundMethod.TRANSFER) {
            paymentMethod = Payment.PaymentMethod.TRANSFER;
        } else {
            paymentMethod = Payment.PaymentMethod.CASH;
        }
        payment.setPaymentMethod(paymentMethod);
        
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setPaymentDate(LocalDateTime.now());
        
        paymentRepository.save(payment);
    }

    /**
     * Send refund notification to customer
     */
    private void sendRefundNotification(User customer, Refund refund) {
        try {
            notificationService.createRefundCompletedNotification(
                customer.getId(),
                refund.getContract().getContractNumber(),
                refund.getRefundAmount(),
                refund.getRefundMethod().toString()
            );
        } catch (Exception e) {
            System.err.println("Failed to send refund notification: " + e.getMessage());
        }
    }
}
