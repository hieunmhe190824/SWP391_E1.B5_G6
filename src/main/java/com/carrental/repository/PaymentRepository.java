package com.carrental.repository;

import com.carrental.model.Contract;
import com.carrental.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByContractId(Long contractId);

    /**
     * Find payment by contract and status
     */
    Optional<Payment> findByContractAndStatus(Contract contract, Payment.PaymentStatus status);

    /**
     * Find payment by transaction reference
     */
    Optional<Payment> findByTransactionRef(String transactionRef);
    
    // ========== ANALYTICS QUERIES FOR REPORTS ==========
    
    /**
     * Calculate total revenue within date range
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "WHERE p.status = 'COMPLETED' " +
           "AND p.paymentDate >= :startDate AND p.paymentDate <= :endDate")
    BigDecimal getTotalRevenue(@Param("startDate") LocalDateTime startDate, 
                               @Param("endDate") LocalDateTime endDate);
    
    /**
     * Calculate revenue by payment type
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "WHERE p.status = 'COMPLETED' " +
           "AND p.paymentType = :paymentType " +
           "AND p.paymentDate >= :startDate AND p.paymentDate <= :endDate")
    BigDecimal getRevenueByType(@Param("paymentType") Payment.PaymentType paymentType,
                                @Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);
    
    /**
     * Calculate revenue by payment method
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "WHERE p.status = 'COMPLETED' " +
           "AND p.paymentMethod = :paymentMethod " +
           "AND p.paymentDate >= :startDate AND p.paymentDate <= :endDate")
    BigDecimal getRevenueByMethod(@Param("paymentMethod") Payment.PaymentMethod paymentMethod,
                                  @Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate);
    
    /**
     * Count payments by status within date range
     */
    @Query("SELECT COUNT(p) FROM Payment p " +
           "WHERE p.status = :status " +
           "AND p.createdAt >= :startDate AND p.createdAt <= :endDate")
    Long countPaymentsByStatus(@Param("status") Payment.PaymentStatus status,
                               @Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get all completed payments within date range for trend analysis
     */
    @Query("SELECT p FROM Payment p " +
           "WHERE p.status = 'COMPLETED' " +
           "AND p.paymentDate >= :startDate AND p.paymentDate <= :endDate " +
           "ORDER BY p.paymentDate ASC")
    List<Payment> findCompletedPaymentsInRange(@Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);
    
    /**
     * Get total pending payment amount
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'PENDING'")
    BigDecimal getTotalPendingAmount();
}
