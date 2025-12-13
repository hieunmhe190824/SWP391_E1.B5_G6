package com.carrental.repository;

import com.carrental.model.TrafficViolation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface TrafficViolationRepository extends JpaRepository<TrafficViolation, Long> {
    List<TrafficViolation> findByContractId(Long contractId);
    List<TrafficViolation> findByDepositHoldId(Long depositHoldId);
    
    @Query("SELECT COALESCE(SUM(v.fineAmount), 0) FROM TrafficViolation v WHERE v.depositHold.id = :depositHoldId")
    BigDecimal sumFineAmountByDepositHoldId(@Param("depositHoldId") Long depositHoldId);
}
