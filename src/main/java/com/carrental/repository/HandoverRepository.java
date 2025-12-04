package com.carrental.repository;

import com.carrental.model.Handover;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HandoverRepository extends JpaRepository<Handover, Long> {
    List<Handover> findByContractId(Long contractId);
}
