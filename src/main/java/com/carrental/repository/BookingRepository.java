package com.carrental.repository;

import com.carrental.model.Booking;
import com.carrental.model.Booking.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByCustomerId(Long customerId);
    List<Booking> findByVehicleId(Long vehicleId);
    List<Booking> findByStatus(BookingStatus status);
}
