package com.carrental.service;

import com.carrental.model.Contract;
import com.carrental.model.Payment;
import com.carrental.model.Payment.PaymentMethod;
import com.carrental.model.Payment.PaymentStatus;
import com.carrental.model.Payment.PaymentType;
import com.carrental.model.User;
import com.carrental.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ContractService contractService;

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public Optional<Payment> getPaymentById(Long id) {
        return paymentRepository.findById(id);
    }

    public List<Payment> getPaymentsByContract(Long contractId) {
        return paymentRepository.findByContractId(contractId);
    }

    public Payment createPayment(Payment payment) {
        payment.setPaymentDate(LocalDateTime.now());
        payment.setStatus(PaymentStatus.PENDING);
        return paymentRepository.save(payment);
    }

    public Payment updatePaymentStatus(Long id, PaymentStatus status) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        payment.setStatus(status);
        return paymentRepository.save(payment);
    }

    public Payment processPayment(Long id, String transactionId) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        payment.setStatus(PaymentStatus.COMPLETED);
        return paymentRepository.save(payment);
    }

    /**
     * Process deposit payment for a contract
     * This is called when customer confirms and pays the deposit
     * @param contractId Contract ID
     * @param customer Customer making the payment
     * @param paymentMethod Payment method (CASH, CARD, TRANSFER)
     * @return Created payment record
     */
    public Payment processDepositPayment(Long contractId, User customer, PaymentMethod paymentMethod) {
        // Get contract
        Contract contract = contractService.getContractById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        // Verify contract belongs to customer
        if (!contract.getCustomer().getId().equals(customer.getId())) {
            throw new RuntimeException("You don't have permission to pay for this contract");
        }

        // Verify contract is in PENDING_PAYMENT status
        if (contract.getStatus() != Contract.ContractStatus.PENDING_PAYMENT) {
            throw new RuntimeException("This contract is not awaiting payment");
        }

        // Check if deposit payment already exists
        List<Payment> existingPayments = paymentRepository.findByContractId(contractId);
        boolean depositExists = existingPayments.stream()
                .anyMatch(p -> p.getPaymentType() == PaymentType.DEPOSIT);

        if (depositExists) {
            throw new RuntimeException("Deposit payment already exists for this contract");
        }

        // Create deposit payment
        Payment payment = new Payment();
        payment.setContract(contract);
        payment.setPaymentType(PaymentType.DEPOSIT);
        payment.setAmount(contract.getDepositAmount());
        payment.setPaymentMethod(paymentMethod);
        payment.setStatus(PaymentStatus.PENDING); // will be finalized by gateway callback
        payment.setPaymentDate(LocalDateTime.now());

        return paymentRepository.save(payment);
    }

    /**
     * Get deposit payment for a contract
     */
    public Optional<Payment> getDepositPayment(Long contractId) {
        return paymentRepository.findByContractId(contractId).stream()
                .filter(p -> p.getPaymentType() == PaymentType.DEPOSIT)
                .findFirst();
    }

    /**
     * Create refund payment when contract is cancelled
     * Used when pickup fails or contract is cancelled before pickup
     * 
     * @param contractId Contract ID
     * @param refundAmount Amount to refund
     * @param reason Refund reason
     * @return Created refund payment
     */
    public Payment createRefundPayment(Long contractId, BigDecimal refundAmount, String reason) {
        // Get contract
        Contract contract = contractService.getContractById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        // Verify contract is cancelled
        if (contract.getStatus() != Contract.ContractStatus.CANCELLED) {
            throw new RuntimeException("Can only create refund for cancelled contracts");
        }

        // Check if deposit payment exists
        Optional<Payment> depositPayment = getDepositPayment(contractId);
        if (depositPayment.isEmpty()) {
            throw new RuntimeException("No deposit payment found for this contract");
        }

        // Create refund payment
        Payment refund = new Payment();
        refund.setContract(contract);
        refund.setPaymentType(PaymentType.REFUND);
        refund.setAmount(refundAmount);
        refund.setPaymentMethod(PaymentMethod.TRANSFER); // Refunds typically via transfer
        refund.setStatus(PaymentStatus.PENDING); // Refund needs to be processed
        refund.setPaymentDate(LocalDateTime.now());

        return paymentRepository.save(refund);
    }
}
