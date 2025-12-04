package com.carrental.service;

import com.carrental.model.DepositHold;
import com.carrental.model.DepositHold.DepositStatus;
import com.carrental.repository.DepositHoldRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
}
