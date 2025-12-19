package com.carrental.service;

import com.carrental.model.Booking;
import com.carrental.model.Contract;
import com.carrental.model.User;
import com.carrental.repository.ContractRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ContractService {

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private NotificationService notificationService;

    // Fixed deposit amount for all cars: 50 million VND
    private static final BigDecimal FIXED_DEPOSIT_AMOUNT = new BigDecimal("50000000");

    public List<Contract> getAllContracts() {
        return contractRepository.findAll();
    }

    /**
     * Get paginated contracts
     */
    public Page<Contract> getContractsPage(Pageable pageable) {
        return contractRepository.findAll(pageable);
    }

    public Optional<Contract> getContractById(Long id) {
        return contractRepository.findById(id);
    }

    public Optional<Contract> getContractByBookingId(Long bookingId) {
        return contractRepository.findByBookingId(bookingId);
    }

    /**
     * Create contract from approved booking
     * This is called when staff approves a booking
     * @param booking The approved booking
     * @param staff The staff member creating the contract
     * @return Created contract with PENDING_PAYMENT status
     */
    public Contract createContractFromBooking(Booking booking, User staff) {
        // Validate booking is approved
        if (booking.getStatus() != Booking.BookingStatus.APPROVED) {
            throw new RuntimeException("Only approved bookings can have contracts created");
        }

        // Check if contract already exists for this booking
        Optional<Contract> existingContract = contractRepository.findByBookingId(booking.getId());
        if (existingContract.isPresent()) {
            throw new RuntimeException("Contract already exists for this booking");
        }

        // Calculate total rental fee
        BigDecimal dailyRate = booking.getVehicle().getDailyRate();
        BigDecimal totalRentalFee = dailyRate.multiply(new BigDecimal(booking.getTotalDays()));

        // Calculate deposit amount: must be STRICTLY greater than total_rental_fee (per constraint chk_deposit_fee)
        // Constraint: deposit_amount > total_rental_fee (strictly greater, not >=)
        // Strategy: Always use max(50M, total_rental_fee * 1.1) + 1 VND to ensure deposit > total_rental_fee
        BigDecimal depositAmount;
        
        // Calculate 110% of total rental fee
        BigDecimal depositFromRentalFee = totalRentalFee.multiply(new BigDecimal("1.1"));
        
        // Use the larger of: fixed deposit (50M) or 110% of rental fee
        if (depositFromRentalFee.compareTo(FIXED_DEPOSIT_AMOUNT) > 0) {
            depositAmount = depositFromRentalFee;
        } else {
            depositAmount = FIXED_DEPOSIT_AMOUNT;
        }
        
        // CRITICAL: Add 1 VND to ensure deposit is STRICTLY greater than total_rental_fee
        // This handles edge cases where deposit might equal total_rental_fee due to rounding
        if (depositAmount.compareTo(totalRentalFee) <= 0) {
            depositAmount = totalRentalFee.add(new BigDecimal("1"));
        } else {
            // Even if deposit > total_rental_fee, add 1 VND as safety margin
            depositAmount = depositAmount.add(new BigDecimal("1"));
        }

        // Create contract
        Contract contract = new Contract();
        contract.setBooking(booking);
        contract.setCustomer(booking.getCustomer());
        contract.setVehicle(booking.getVehicle());
        contract.setStaff(staff);
        contract.setContractNumber(generateContractNumber());
        contract.setStartDate(booking.getStartDate());
        contract.setEndDate(booking.getEndDate());
        contract.setTotalDays(booking.getTotalDays());
        contract.setDailyRate(dailyRate);
        contract.setTotalRentalFee(totalRentalFee);
        contract.setDepositAmount(depositAmount); // Dynamic deposit: max(50M, total_rental_fee * 1.1)
        contract.setStatus(Contract.ContractStatus.PENDING_PAYMENT); // Waiting for deposit payment

        Contract savedContract = contractRepository.save(contract);

        // Send notification to customer about contract creation
        try {
            notificationService.createContractCreatedNotification(
                booking.getCustomer().getId(),
                savedContract.getContractNumber(),
                depositAmount
            );
        } catch (Exception e) {
            System.err.println("Failed to send contract created notification: " + e.getMessage());
        }

        return savedContract;
    }

    public Contract createContract(Contract contract) {
        contract.setContractNumber(generateContractNumber());
        contract.setStatus(Contract.ContractStatus.ACTIVE);
        return contractRepository.save(contract);
    }

    public Contract updateContractStatus(Long id, Contract.ContractStatus status) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contract not found"));
        
        Contract.ContractStatus oldStatus = contract.getStatus();
        contract.setStatus(status);
        Contract savedContract = contractRepository.save(contract);

        // Send notifications based on status change
        try {
            if (status == Contract.ContractStatus.ACTIVE && oldStatus != Contract.ContractStatus.ACTIVE) {
                // Contract activated (deposit paid)
                notificationService.createContractActivatedNotification(
                    contract.getCustomer().getId(),
                    contract.getContractNumber(),
                    contract.getStartDate().toString(),
                    contract.getEndDate().toString()
                );
            } else if (status == Contract.ContractStatus.CANCELLED && oldStatus != Contract.ContractStatus.CANCELLED) {
                // Contract cancelled
                notificationService.createContractCancelledNotification(
                    contract.getCustomer().getId(),
                    contract.getContractNumber(),
                    "Hợp đồng đã bị hủy bởi hệ thống"
                );
            }
        } catch (Exception e) {
            System.err.println("Failed to send contract status notification: " + e.getMessage());
        }

        return savedContract;
    }

    /**
     * Get contracts by customer
     */
    public List<Contract> getContractsByCustomer(Long customerId) {
        return contractRepository.findAll().stream()
                .filter(c -> c.getCustomer() != null && c.getCustomer().getId().equals(customerId))
                .toList();
    }

    /**
     * Get contracts by status
     */
    public List<Contract> getContractsByStatus(Contract.ContractStatus status) {
        return contractRepository.findAll().stream()
                .filter(c -> c.getStatus() == status)
                .toList();
    }

    /**
     * Get paginated contracts by status
     */
    public Page<Contract> getContractsByStatusPage(Contract.ContractStatus status, Pageable pageable) {
        return contractRepository.findByStatus(status, pageable);
    }

    /**
     * Generate unique contract number
     * Format: HD-YYYYMMDD-XXXXXX (max 50 chars)
     * Example: HD-20251219-A1B2C3
     */
    private String generateContractNumber() {
        // Use date format YYYYMMDD instead of timestamp to keep it shorter
        java.time.LocalDate today = java.time.LocalDate.now();
        String dateStr = today.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        // Use shorter random string (6 chars instead of 8)
        String randomStr = UUID.randomUUID().toString().substring(0, 6).toUpperCase().replace("-", "");
        
        // Format: HD-YYYYMMDD-XXXXXX (total: 3 + 1 + 8 + 1 + 6 = 19 chars, well under 50 limit)
        return "HD-" + dateStr + "-" + randomStr;
    }
}
