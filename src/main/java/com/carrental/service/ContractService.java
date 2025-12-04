package com.carrental.service;

import com.carrental.model.Contract;
import com.carrental.repository.ContractRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ContractService {

    @Autowired
    private ContractRepository contractRepository;

    public List<Contract> getAllContracts() {
        return contractRepository.findAll();
    }

    public Optional<Contract> getContractById(Long id) {
        return contractRepository.findById(id);
    }

    public Optional<Contract> getContractByBookingId(Long bookingId) {
        return contractRepository.findByBookingId(bookingId);
    }

    public Contract createContract(Contract contract) {
        contract.setContractNumber(generateContractNumber());
        contract.setStatus(Contract.ContractStatus.ACTIVE);
        return contractRepository.save(contract);
    }

    public Contract updateContractStatus(Long id, Contract.ContractStatus status) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contract not found"));
        contract.setStatus(status);
        return contractRepository.save(contract);
    }

    private String generateContractNumber() {
        return "CTR-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
