package com.carrental.repository;

import com.carrental.model.Contract;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    List<Contract> findByStatus(Contract.ContractStatus status);

    // Paginated contracts by status
    Page<Contract> findByStatus(Contract.ContractStatus status, Pageable pageable);
    
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

    /**
     * Check if vehicle has active contracts (ACTIVE or PENDING_PAYMENT) that overlap with date range
     * Used to determine if vehicle is available for booking
     */
    @Query("SELECT COUNT(c) FROM Contract c " +
           "WHERE c.vehicle.id = :vehicleId " +
           "AND c.status IN ('ACTIVE', 'PENDING_PAYMENT') " +
           "AND ((c.startDate <= :endDate AND c.endDate >= :startDate))")
    long countActiveContractsForDateRange(@Param("vehicleId") Long vehicleId,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    /**
     * Check if vehicle has any active contracts (ACTIVE or PENDING_PAYMENT) at current time
     */
    @Query("SELECT COUNT(c) FROM Contract c " +
           "WHERE c.vehicle.id = :vehicleId " +
           "AND c.status IN ('ACTIVE', 'PENDING_PAYMENT') " +
           "AND c.startDate <= :currentTime AND c.endDate >= :currentTime")
    long countActiveContractsAtTime(@Param("vehicleId") Long vehicleId,
                                    @Param("currentTime") LocalDateTime currentTime);

    /**
     * Get all active contracts (ACTIVE or PENDING_PAYMENT) for a vehicle within a date range
     * Used to display availability calendar
     */
    @Query("SELECT c FROM Contract c " +
           "WHERE c.vehicle.id = :vehicleId " +
           "AND c.status IN ('ACTIVE', 'PENDING_PAYMENT') " +
           "AND c.endDate >= :startDate " +
           "ORDER BY c.startDate ASC")
    List<Contract> findActiveContractsInRange(@Param("vehicleId") Long vehicleId,
                                              @Param("startDate") LocalDateTime startDate);
}
