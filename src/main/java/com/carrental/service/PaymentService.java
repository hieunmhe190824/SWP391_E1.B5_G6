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
     * Create rental payment (for rental fees after return)
     * @param contractId Contract ID
     * @param amount Rental amount (total rental fee + any additional fees)
     * @param paymentMethod Payment method
     * @return Created payment record
     */
    public Payment createRentalPayment(Long contractId, BigDecimal amount, PaymentMethod paymentMethod) {
        // Get contract
        Contract contract = contractService.getContractById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        // Verify contract is waiting for bill payment
        if (contract.getStatus() != Contract.ContractStatus.BILL_PENDING
                && contract.getStatus() != Contract.ContractStatus.COMPLETED) {
            throw new RuntimeException("Can only create rental payment for contracts waiting for bill payment");
        }

        // Check if rental payment already exists
        List<Payment> existingPayments = paymentRepository.findByContractId(contractId);
        boolean rentalExists = existingPayments.stream()
                .anyMatch(p -> p.getPaymentType() == PaymentType.RENTAL);

        if (rentalExists) {
            throw new RuntimeException("Rental payment already exists for this contract");
        }

        // Create rental payment
        Payment payment = new Payment();
        payment.setContract(contract);
        payment.setPaymentType(PaymentType.RENTAL);
        payment.setAmount(amount);
        payment.setPaymentMethod(paymentMethod);
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaymentDate(LocalDateTime.now());

        Payment savedPayment = paymentRepository.save(payment);

        // When rental payment is processed manually, mark contract as completed
        if (contract.getStatus() != Contract.ContractStatus.COMPLETED) {
            contract.setStatus(Contract.ContractStatus.COMPLETED);
            contractService.updateContractStatus(contractId, Contract.ContractStatus.COMPLETED);
        }

        return savedPayment;
    }

    /**
     * Get rental payment for a contract
     */
    public Optional<Payment> getRentalPayment(Long contractId) {
        return paymentRepository.findByContractId(contractId).stream()
                .filter(p -> p.getPaymentType() == PaymentType.RENTAL)
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

    /**
     * Create bill payment after vehicle return (replaces BillService)
     * This creates a RENTAL payment with detailed bill information
     * @param contractId Contract ID
     * @param returnFee ReturnFee containing fee details
     * @return Created payment record with bill details
     */
    public Payment createBillPaymentAfterReturn(Long contractId, com.carrental.model.ReturnFee returnFee) {
        // Check if rental payment already exists
        Optional<Payment> existingPayment = getRentalPayment(contractId);
        if (existingPayment.isPresent()) {
            return existingPayment.get();
        }

        Contract contract = contractService.getContractById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        // Calculate rental adjustment (difference between actual and original)
        // returnFee.getTotalFees() = rentalAdjustment + lateFee + damageFee + oneWayFee
        BigDecimal rentalAdjustment = returnFee.getTotalFees()
                .subtract(returnFee.getLateFee() != null ? returnFee.getLateFee() : BigDecimal.ZERO)
                .subtract(returnFee.getDamageFee() != null ? returnFee.getDamageFee() : BigDecimal.ZERO)
                .subtract(returnFee.getOneWayFee() != null ? returnFee.getOneWayFee() : BigDecimal.ZERO);

        // Calculate actual rental fee = original + adjustment
        BigDecimal actualRentalFee = contract.getTotalRentalFee().add(rentalAdjustment);

        // Calculate total additional fees (late + damage + one-way)
        BigDecimal totalAdditionalFees = (returnFee.getLateFee() != null ? returnFee.getLateFee() : BigDecimal.ZERO)
                .add(returnFee.getDamageFee() != null ? returnFee.getDamageFee() : BigDecimal.ZERO)
                .add(returnFee.getOneWayFee() != null ? returnFee.getOneWayFee() : BigDecimal.ZERO);

        // Total amount = actual rental fee + additional fees
        BigDecimal totalAmount = actualRentalFee.add(totalAdditionalFees);

        // Generate bill number
        String billNumber = generateBillNumber();

        // Create rental payment with bill details
        Payment payment = new Payment();
        payment.setContract(contract);
        payment.setPaymentType(PaymentType.RENTAL);
        payment.setAmount(totalAmount);
        payment.setPaymentMethod(PaymentMethod.ONLINE); // Default, can be changed when customer pays
        payment.setStatus(PaymentStatus.PENDING); // Waiting for customer payment
        payment.setPaymentDate(null); // Will be set when paid

        // Set bill details
        payment.setBillNumber(billNumber);
        payment.setOriginalRentalFee(contract.getTotalRentalFee());
        payment.setRentalAdjustment(rentalAdjustment);
        payment.setActualRentalFee(actualRentalFee);
        payment.setLateFee(returnFee.getLateFee() != null ? returnFee.getLateFee() : BigDecimal.ZERO);
        payment.setDamageFee(returnFee.getDamageFee() != null ? returnFee.getDamageFee() : BigDecimal.ZERO);
        payment.setOneWayFee(returnFee.getOneWayFee() != null ? returnFee.getOneWayFee() : BigDecimal.ZERO);
        payment.setTotalAdditionalFees(totalAdditionalFees);
        payment.setDepositAmount(contract.getDepositAmount()); // 50,000,000 VND - still held
        payment.setAmountPaid(BigDecimal.ZERO);
        payment.setAmountDue(totalAmount);
        payment.setNotes("Hóa đơn được tạo tự động sau khi trả xe. Tiền cọc 50,000,000 VND vẫn được giữ lại.");

        return paymentRepository.save(payment);
    }

    /**
     * Generate unique bill number (format: BILL-YYYYMMDD-XXXX)
     */
    private String generateBillNumber() {
        String datePrefix = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = String.format("%04d", (int)(Math.random() * 10000));
        String billNumber = "BILL-" + datePrefix + "-" + randomSuffix;
        
        // Ensure uniqueness
        while (paymentRepository.findByBillNumber(billNumber).isPresent()) {
            randomSuffix = String.format("%04d", (int)(Math.random() * 10000));
            billNumber = "BILL-" + datePrefix + "-" + randomSuffix;
        }
        
        return billNumber;
    }

    /**
     * Get bill payment (RENTAL payment with bill details) by contract ID
     */
    public Optional<Payment> getBillPaymentByContractId(Long contractId) {
        return getRentalPayment(contractId);
    }
}
