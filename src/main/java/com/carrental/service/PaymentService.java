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
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaymentDate(LocalDateTime.now());

        Payment savedPayment = paymentRepository.save(payment);

        // Update contract status to ACTIVE
        contractService.updateContractStatus(contractId, Contract.ContractStatus.ACTIVE);

        return savedPayment;
    }

    /**
     * Get deposit payment for a contract
     */
    public Optional<Payment> getDepositPayment(Long contractId) {
        return paymentRepository.findByContractId(contractId).stream()
                .filter(p -> p.getPaymentType() == PaymentType.DEPOSIT)
                .findFirst();
    }
}
