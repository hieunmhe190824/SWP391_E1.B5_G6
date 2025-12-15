package com.carrental.service;

import com.carrental.model.Booking;
import com.carrental.model.Contract;
import com.carrental.model.User;
import com.carrental.repository.ContractRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
        contract.setDepositAmount(FIXED_DEPOSIT_AMOUNT); // Fixed 50M VND deposit
        contract.setStatus(Contract.ContractStatus.PENDING_PAYMENT); // Waiting for deposit payment

        Contract savedContract = contractRepository.save(contract);

        // Send notification to customer about contract creation
        try {
            notificationService.createContractCreatedNotification(
                booking.getCustomer().getId(),
                savedContract.getContractNumber(),
                FIXED_DEPOSIT_AMOUNT
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

    private String generateContractNumber() {
        return "CTR-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
