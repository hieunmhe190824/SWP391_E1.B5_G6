package com.carrental.repository;

import com.carrental.model.Booking;
import com.carrental.model.Booking.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * Find bookings by customer ID
     */
    List<Booking> findByCustomerId(Long customerId);

    /**
     * Find bookings by vehicle ID
     */
    List<Booking> findByVehicleId(Long vehicleId);

    /**
     * Find bookings by status
     */
    List<Booking> findByStatus(BookingStatus status);

    /**
     * Find booking by ID with all relationships loaded
     * Prevents LazyInitializationException
     */
    @Query("SELECT b FROM Booking b " +
           "LEFT JOIN FETCH b.customer " +
           "LEFT JOIN FETCH b.vehicle v " +
           "LEFT JOIN FETCH v.model m " +
           "LEFT JOIN FETCH m.brand " +
           "LEFT JOIN FETCH b.pickupLocation " +
           "LEFT JOIN FETCH b.returnLocation " +
           "WHERE b.id = :id")
    Optional<Booking> findByIdWithRelations(@Param("id") Long id);

    /**
     * Find all bookings with relationships loaded
     * Ordered by created date descending (newest first)
     */
    @Query("SELECT DISTINCT b FROM Booking b " +
           "LEFT JOIN FETCH b.customer " +
           "LEFT JOIN FETCH b.vehicle v " +
           "LEFT JOIN FETCH v.model m " +
           "LEFT JOIN FETCH m.brand " +
           "LEFT JOIN FETCH b.pickupLocation " +
           "LEFT JOIN FETCH b.returnLocation " +
           "ORDER BY b.createdAt DESC")
    List<Booking> findAllWithRelations();

    /**
     * Find bookings by customer ID with relationships loaded
     * Ordered by created date descending
     */
    @Query("SELECT b FROM Booking b " +
           "LEFT JOIN FETCH b.customer " +
           "LEFT JOIN FETCH b.vehicle v " +
           "LEFT JOIN FETCH v.model m " +
           "LEFT JOIN FETCH m.brand " +
           "LEFT JOIN FETCH b.pickupLocation " +
           "LEFT JOIN FETCH b.returnLocation " +
           "WHERE b.customer.id = :customerId " +
           "ORDER BY b.createdAt DESC")
    List<Booking> findByCustomerIdWithRelations(@Param("customerId") Long customerId);

    /**
     * Find bookings by status with relationships loaded
     */
    @Query("SELECT b FROM Booking b " +
           "LEFT JOIN FETCH b.customer " +
           "LEFT JOIN FETCH b.vehicle v " +
           "LEFT JOIN FETCH v.model m " +
           "LEFT JOIN FETCH m.brand " +
           "LEFT JOIN FETCH b.pickupLocation " +
           "LEFT JOIN FETCH b.returnLocation " +
           "WHERE b.statusString = :status " +
           "ORDER BY b.createdAt DESC")
    List<Booking> findByStatusStringWithRelations(@Param("status") String status);

    /**
     * Find pending bookings (for staff to review)
     */
    @Query("SELECT b FROM Booking b " +
           "LEFT JOIN FETCH b.customer " +
           "LEFT JOIN FETCH b.vehicle v " +
           "LEFT JOIN FETCH v.model m " +
           "LEFT JOIN FETCH m.brand " +
           "LEFT JOIN FETCH b.pickupLocation " +
           "LEFT JOIN FETCH b.returnLocation " +
           "WHERE b.statusString = 'Pending' " +
           "ORDER BY b.createdAt ASC")
    List<Booking> findPendingBookingsWithRelations();

    /**
     * Check if vehicle is available for booking in a date range
     * Returns count of conflicting bookings (should be 0 for available)
     */
    @Query("SELECT COUNT(b) FROM Booking b " +
           "WHERE b.vehicle.id = :vehicleId " +
           "AND b.statusString IN ('Pending', 'Approved') " +
           "AND ((b.startDate <= :endDate AND b.endDate >= :startDate))")
    long countConflictingBookings(@Param("vehicleId") Long vehicleId,
                                  @Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate);

    /**
     * Find bookings by customer ID and status
     */
    @Query("SELECT b FROM Booking b " +
           "LEFT JOIN FETCH b.customer " +
           "LEFT JOIN FETCH b.vehicle v " +
           "LEFT JOIN FETCH v.model m " +
           "LEFT JOIN FETCH m.brand " +
           "LEFT JOIN FETCH b.pickupLocation " +
           "LEFT JOIN FETCH b.returnLocation " +
           "WHERE b.customer.id = :customerId " +
           "AND b.statusString = :status " +
           "ORDER BY b.createdAt DESC")
    List<Booking> findByCustomerIdAndStatusWithRelations(@Param("customerId") Long customerId,
                                                         @Param("status") String status);
}
