package com.carrental.repository;

import com.carrental.model.DepositHold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepositHoldRepository extends JpaRepository<DepositHold, Long> {
    Optional<DepositHold> findByContractId(Long contractId);
}
