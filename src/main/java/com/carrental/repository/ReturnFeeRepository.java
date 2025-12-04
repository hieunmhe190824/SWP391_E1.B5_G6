package com.carrental.repository;

import com.carrental.model.ReturnFee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnFeeRepository extends JpaRepository<ReturnFee, Long> {
    List<ReturnFee> findByHandoverId(Long handoverId);
}
