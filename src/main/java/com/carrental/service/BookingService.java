package com.carrental.service;

import com.carrental.model.Booking;
import com.carrental.model.Booking.BookingStatus;
import com.carrental.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private VehicleService vehicleService;

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public Optional<Booking> getBookingById(Long id) {
        return bookingRepository.findById(id);
    }

    public List<Booking> getBookingsByCustomer(Long customerId) {
        return bookingRepository.findByCustomerId(customerId);
    }

    public List<Booking> getBookingsByVehicle(Long vehicleId) {
        return bookingRepository.findByVehicleId(vehicleId);
    }

    public Booking createBooking(Booking booking) {
        booking.setStatus(BookingStatus.PENDING);
        booking.setCreatedAt(LocalDateTime.now());
        return bookingRepository.save(booking);
    }

    public Booking updateBookingStatus(Long id, BookingStatus status) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        booking.setStatus(status);
        return bookingRepository.save(booking);
    }

    public Booking confirmBooking(Long id) {
        Booking booking = updateBookingStatus(id, BookingStatus.APPROVED);
        vehicleService.updateVehicleStatus(booking.getVehicle().getId(),
                com.carrental.model.Vehicle.VehicleStatus.RENTED);
        return booking;
    }

    public Booking cancelBooking(Long id) {
        Booking booking = updateBookingStatus(id, BookingStatus.CANCELLED);
        vehicleService.updateVehicleStatus(booking.getVehicle().getId(),
                com.carrental.model.Vehicle.VehicleStatus.AVAILABLE);
        return booking;
    }

    public void deleteBooking(Long id) {
        bookingRepository.deleteById(id);
    }
}
