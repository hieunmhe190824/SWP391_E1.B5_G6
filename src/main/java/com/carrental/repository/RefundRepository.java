package com.carrental.repository;

import com.carrental.model.Refund;
import com.carrental.model.Refund.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {
    List<Refund> findByContractId(Long contractId);
    Optional<Refund> findByDepositHoldId(Long depositHoldId);
    List<Refund> findByStatus(RefundStatus status);
}
