package com.carrental.repository;

import com.carrental.model.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {
    Optional<Contract> findByBookingId(Long bookingId);
    Optional<Contract> findByContractNumber(String contractNumber);
}
