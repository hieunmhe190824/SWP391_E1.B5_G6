package com.carrental.repository;

import com.carrental.model.Handover;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HandoverRepository extends JpaRepository<Handover, Long> {
    List<Handover> findByContractId(Long contractId);
    
    Optional<Handover> findByContractIdAndType(Long contractId, Handover.HandoverType type);
    
    List<Handover> findByType(Handover.HandoverType type);
}
