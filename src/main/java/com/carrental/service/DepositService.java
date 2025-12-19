package com.carrental.service;

import com.carrental.model.DepositHold;
import com.carrental.model.DepositHold.DepositStatus;
import com.carrental.repository.DepositHoldRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DepositService {

    @Autowired
    private DepositHoldRepository depositHoldRepository;

    public Optional<DepositHold> getDepositByContractId(Long contractId) {
        return depositHoldRepository.findByContractId(contractId);
    }

    public DepositHold createDepositHold(DepositHold deposit) {
        deposit.setHoldStartDate(LocalDateTime.now());
        deposit.setStatus(DepositStatus.HOLDING);
        return depositHoldRepository.save(deposit);
    }

    public DepositHold releaseDeposit(Long id) {
        DepositHold deposit = depositHoldRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Deposit not found"));
        deposit.setHoldEndDate(LocalDateTime.now());
        deposit.setStatus(DepositStatus.READY);
        return depositHoldRepository.save(deposit);
    }

    public DepositHold deductDeposit(Long id) {
        DepositHold deposit = depositHoldRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Deposit not found"));
        deposit.setHoldEndDate(LocalDateTime.now());
        deposit.setStatus(DepositStatus.REFUNDED);
        return depositHoldRepository.save(deposit);
    }

    /**
     * Get deposits ready for refund (14 days passed)
     */
    public List<DepositHold> getDepositsReadyForRefund() {
        return depositHoldRepository.findByHoldEndDateBeforeAndStatus(
            LocalDateTime.now(), 
            DepositStatus.HOLDING
        );
    }

    /**
     * Get deposits by status
     */
    public List<DepositHold> getDepositsByStatus(DepositStatus status) {
        return depositHoldRepository.findByStatus(status);
    }

    /**
     * Get all deposit holds
     */
    public List<DepositHold> getAllDeposits() {
        return depositHoldRepository.findAll();
    }

    /**
     * Get deposits by booking assigned staff ID (through contract.booking.assigned_staff_id)
     * 
     * @param assignedStaffId Staff user ID (booking.assigned_staff_id)
     * @return List of deposit holds for contracts whose bookings are assigned to the staff
     */
    public List<DepositHold> getDepositsByStaffId(Long assignedStaffId) {
        return depositHoldRepository.findAll().stream()
                .filter(deposit -> deposit.getContract() != null 
                        && deposit.getContract().getBooking() != null
                        && deposit.getContract().getBooking().getAssignedStaff() != null
                        && deposit.getContract().getBooking().getAssignedStaff().getId().equals(assignedStaffId))
                .toList();
    }

    /**
     * Get deposits by booking assigned staff ID and status
     * 
     * @param assignedStaffId Staff user ID (booking.assigned_staff_id)
     * @param status Deposit status
     * @return List of deposit holds for contracts whose bookings are assigned to the staff with specific status
     */
    public List<DepositHold> getDepositsByStaffIdAndStatus(Long assignedStaffId, DepositStatus status) {
        return depositHoldRepository.findByStatus(status).stream()
                .filter(deposit -> deposit.getContract() != null 
                        && deposit.getContract().getBooking() != null
                        && deposit.getContract().getBooking().getAssignedStaff() != null
                        && deposit.getContract().getBooking().getAssignedStaff().getId().equals(assignedStaffId))
                .toList();
    }

    /**
     * Get deposit hold by ID
     */
    public Optional<DepositHold> getDepositById(Long id) {
        return depositHoldRepository.findById(id);
    }

    /**
     * Update deposit status
     */
    public DepositHold updateDepositStatus(Long holdId, DepositStatus status) {
        DepositHold deposit = depositHoldRepository.findById(holdId)
                .orElseThrow(() -> new RuntimeException("Deposit hold not found"));
        deposit.setStatus(status);
        return depositHoldRepository.save(deposit);
    }
}

