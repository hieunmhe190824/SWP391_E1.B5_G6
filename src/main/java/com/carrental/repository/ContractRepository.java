package com.carrental.repository;

import com.carrental.model.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {
    Optional<Contract> findByBookingId(Long bookingId);
    Optional<Contract> findByContractNumber(String contractNumber);
    
    // ========== ANALYTICS QUERIES FOR REPORTS ==========
    
    /**
     * Count contracts by status
     */
    @Query("SELECT COUNT(c) FROM Contract c WHERE c.status = :status")
    Long countByStatus(@Param("status") Contract.ContractStatus status);
    
    /**
     * Find contracts within date range
     */
    @Query("SELECT c FROM Contract c " +
           "WHERE c.createdAt >= :startDate AND c.createdAt <= :endDate " +
           "ORDER BY c.createdAt DESC")
    List<Contract> findContractsInRange(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);
    
    /**
     * Count contracts by status within date range
     */
    @Query("SELECT COUNT(c) FROM Contract c " +
           "WHERE c.status = :status " +
           "AND c.createdAt >= :startDate AND c.createdAt <= :endDate")
    Long countByStatusInRange(@Param("status") Contract.ContractStatus status,
                              @Param("startDate") LocalDateTime startDate,
                              @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find contracts by vehicle for usage statistics
     */
    @Query("SELECT c FROM Contract c " +
           "WHERE c.vehicle.id = :vehicleId " +
           "AND c.status IN ('ACTIVE', 'COMPLETED') " +
           "ORDER BY c.createdAt DESC")
    List<Contract> findByVehicleIdAndActiveOrCompleted(@Param("vehicleId") Long vehicleId);
    
    /**
     * Get all contracts with vehicle details for usage report
     */
    @Query("SELECT c FROM Contract c " +
           "LEFT JOIN FETCH c.vehicle v " +
           "LEFT JOIN FETCH v.model m " +
           "LEFT JOIN FETCH m.brand b " +
           "WHERE c.status IN ('ACTIVE', 'COMPLETED') " +
           "AND c.createdAt >= :startDate AND c.createdAt <= :endDate")
    List<Contract> findActiveAndCompletedContractsWithVehicle(@Param("startDate") LocalDateTime startDate,
                                                              @Param("endDate") LocalDateTime endDate);
}
