package com.carrental.repository;

import com.carrental.model.DepositHold;
import com.carrental.model.DepositHold.DepositStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DepositHoldRepository extends JpaRepository<DepositHold, Long> {
    Optional<DepositHold> findByContractId(Long contractId);
    List<DepositHold> findByStatus(DepositStatus status);
    List<DepositHold> findByHoldEndDateBeforeAndStatus(LocalDateTime date, DepositStatus status);
}
