package com.carrental.repository;

import com.carrental.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    
    // ========== ANALYTICS QUERIES FOR REPORTS ==========
    
    /**
     * Count customers (users with CUSTOMER role)
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = 'CUSTOMER'")
    Long countCustomers();
    
    /**
     * Count new customers within date range
     */
    @Query("SELECT COUNT(u) FROM User u " +
           "WHERE u.role = 'CUSTOMER' " +
           "AND u.createdAt >= :startDate AND u.createdAt <= :endDate")
    Long countNewCustomers(@Param("startDate") LocalDateTime startDate,
                          @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find all customers with their creation date
     */
    @Query("SELECT u FROM User u WHERE u.role = 'CUSTOMER' ORDER BY u.createdAt DESC")
    List<User> findAllCustomers();
    
    /**
     * Count active customers (those who have made bookings)
     */
    @Query("SELECT COUNT(DISTINCT b.customer.id) FROM Booking b")
    Long countActiveCustomers();
}
