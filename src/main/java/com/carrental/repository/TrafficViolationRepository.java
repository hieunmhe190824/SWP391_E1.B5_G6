package com.carrental.repository;

import com.carrental.model.TrafficViolation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrafficViolationRepository extends JpaRepository<TrafficViolation, Long> {
    List<TrafficViolation> findByContractId(Long contractId);
}
