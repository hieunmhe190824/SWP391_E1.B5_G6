package com.carrental.service;

import com.carrental.controller.BookingController.BookingCreateDTO;
import com.carrental.model.*;
import com.carrental.model.Booking.BookingStatus;
import com.carrental.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Enhanced Booking Service
 * Handles all booking operations including document management
 */
@Service
@Transactional
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingDocumentRepository bookingDocumentRepository;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private UserDocumentRepository userDocumentRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get all bookings with relationships loaded
     */
    public List<Booking> getAllBookings() {
        return bookingRepository.findAllWithRelations();
    }

    /**
     * Get booking by ID with relationships
     */
    public Optional<Booking> getBookingById(Long id) {
        return bookingRepository.findByIdWithRelations(id);
    }

    /**
     * Get bookings by customer with relationships
     */
    public List<Booking> getBookingsByCustomer(Long customerId) {
        return bookingRepository.findByCustomerIdWithRelations(customerId);
    }

    /**
     * Get bookings by vehicle
     */
    public List<Booking> getBookingsByVehicle(Long vehicleId) {
        return bookingRepository.findByVehicleId(vehicleId);
    }

    /**
     * Get pending bookings for staff review
     */
    public List<Booking> getPendingBookings() {
        return bookingRepository.findPendingBookingsWithRelations();
    }

    /**
     * Get bookings by status
     */
    public List<Booking> getBookingsByStatus(BookingStatus status) {
        return bookingRepository.findByStatusStringWithRelations(status.getValue());
    }

    /**
     * UC06: Create new booking with documents
     * @param dto Booking creation data
     * @param customer Current logged-in customer
     * @return Created booking
     */
    public Booking createBooking(BookingCreateDTO dto, User customer) {
        // Validate vehicle exists and is available
        Vehicle vehicle = vehicleRepository.findByIdWithRelations(dto.getVehicleId())
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        if (vehicle.getStatus() != Vehicle.VehicleStatus.Available) {
            throw new RuntimeException("Vehicle is not available for booking");
        }

        // Validate locations
        Location pickupLocation = locationRepository.findById(dto.getPickupLocationId())
                .orElseThrow(() -> new RuntimeException("Pickup location not found"));
        Location returnLocation = locationRepository.findById(dto.getReturnLocationId())
                .orElseThrow(() -> new RuntimeException("Return location not found"));

        // Validate dates
        if (dto.getStartDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Start date cannot be in the past");
        }
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new RuntimeException("End date must be after start date");
        }

        // Check for conflicting bookings
        long conflicts = bookingRepository.countConflictingBookings(
                dto.getVehicleId(), dto.getStartDate(), dto.getEndDate());
        if (conflicts > 0) {
            throw new RuntimeException("Vehicle is already booked for the selected dates");
        }

        // Calculate total days
        long totalDays = ChronoUnit.DAYS.between(
                dto.getStartDate().toLocalDate(),
                dto.getEndDate().toLocalDate());
        if (totalDays < 1) {
            totalDays = 1; // Minimum 1 day
        }

        // Create booking
        Booking booking = new Booking();
        booking.setCustomer(customer);
        booking.setVehicle(vehicle);
        booking.setPickupLocation(pickupLocation);
        booking.setReturnLocation(returnLocation);
        booking.setStartDate(dto.getStartDate());
        booking.setEndDate(dto.getEndDate());
        booking.setTotalDays((int) totalDays);
        booking.setStatus(BookingStatus.PENDING);

        // Save booking
        booking = bookingRepository.save(booking);

        // Attach documents if provided
        if (dto.getDocumentIds() != null && !dto.getDocumentIds().isEmpty()) {
            for (Long documentId : dto.getDocumentIds()) {
                UserDocument document = userDocumentRepository.findById(documentId)
                        .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

                // Verify document belongs to customer
                if (!document.getUser().getId().equals(customer.getId())) {
                    throw new RuntimeException("Document does not belong to customer");
                }

                // Create booking-document relationship
                BookingDocument bookingDocument = new BookingDocument(booking, document);
                bookingDocumentRepository.save(bookingDocument);
            }
        }

        // Reload booking with all relationships to avoid LazyInitializationException
        // This ensures vehicle, model, brand, and locations are loaded for the success message
        return bookingRepository.findByIdWithRelations(booking.getId())
                .orElse(booking);
    }

    /**
     * UC07: Approve booking (Staff only)
     * Verifies documents and approves the booking
     * @param bookingId Booking ID to approve
     * @param staffUser Staff user performing the action
     * @return Approved booking
     */
    public Booking approveBooking(Long bookingId, User staffUser) {
        Booking booking = bookingRepository.findByIdWithRelations(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // Validate booking status
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException("Only pending bookings can be approved");
        }

        // Check if booking has required documents
        List<BookingDocument> bookingDocuments = bookingDocumentRepository.findByBookingIdWithDocuments(bookingId);
        if (bookingDocuments.isEmpty()) {
            throw new RuntimeException("Booking must have at least one document attached");
        }

        // Verify all documents are approved
        boolean allDocumentsApproved = bookingDocuments.stream()
                .allMatch(bd -> bd.getDocument().getStatus() == UserDocument.DocumentStatus.Approved);

        if (!allDocumentsApproved) {
            throw new RuntimeException("All documents must be approved before booking can be approved");
        }

        // Update booking status
        booking.setStatus(BookingStatus.APPROVED);

        // Note: Vehicle status will be updated to RENTED when contract is created
        // For now, we keep it as AVAILABLE until the actual rental starts

        return bookingRepository.save(booking);
    }

    /**
     * UC07: Reject booking (Staff only)
     * @param bookingId Booking ID to reject
     * @param staffUser Staff user performing the action
     * @param reason Rejection reason (optional)
     * @return Rejected booking
     */
    public Booking rejectBooking(Long bookingId, User staffUser, String reason) {
        Booking booking = bookingRepository.findByIdWithRelations(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // Validate booking status
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException("Only pending bookings can be rejected");
        }

        // Update booking status
        booking.setStatus(BookingStatus.REJECTED);

        // TODO: Send notification to customer with rejection reason

        return bookingRepository.save(booking);
    }

    /**
     * UC08: Cancel booking (Customer or Staff)
     * Can only cancel if no contract has been created yet
     * @param bookingId Booking ID to cancel
     * @param user User performing the action
     * @return Cancelled booking
     */
    public Booking cancelBooking(Long bookingId, User user) {
        Booking booking = bookingRepository.findByIdWithRelations(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // Validate user has permission to cancel
        if (user.getRole() == User.UserRole.CUSTOMER) {
            if (!booking.getCustomer().getId().equals(user.getId())) {
                throw new RuntimeException("You can only cancel your own bookings");
            }
        }

        // Validate booking status - can only cancel Pending or Approved bookings
        if (booking.getStatus() != BookingStatus.PENDING &&
            booking.getStatus() != BookingStatus.APPROVED) {
            throw new RuntimeException("Only pending or approved bookings can be cancelled");
        }

        // TODO: Check if contract exists - if yes, cannot cancel
        // For now, we assume no contract exists

        // Update booking status
        booking.setStatus(BookingStatus.CANCELLED);

        return bookingRepository.save(booking);
    }

    /**
     * Cancel booking by ID (simplified version for backward compatibility)
     */
    public Booking cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findByIdWithRelations(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        booking.setStatus(BookingStatus.CANCELLED);
        return bookingRepository.save(booking);
    }

    /**
     * Update booking status
     */
    public Booking updateBookingStatus(Long id, BookingStatus status) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        booking.setStatus(status);
        return bookingRepository.save(booking);
    }

    /**
     * Get booking documents
     */
    public List<UserDocument> getBookingDocuments(Long bookingId) {
        List<BookingDocument> bookingDocuments = bookingDocumentRepository.findByBookingIdWithDocuments(bookingId);
        return bookingDocuments.stream()
                .map(BookingDocument::getDocument)
                .collect(Collectors.toList());
    }

    /**
     * Delete booking (Admin only)
     */
    public void deleteBooking(Long id) {
        bookingRepository.deleteById(id);
    }

    /**
     * Check if vehicle is available for booking
     */
    public boolean isVehicleAvailable(Long vehicleId, LocalDateTime startDate, LocalDateTime endDate) {
        long conflicts = bookingRepository.countConflictingBookings(vehicleId, startDate, endDate);
        return conflicts == 0;
    }
}
