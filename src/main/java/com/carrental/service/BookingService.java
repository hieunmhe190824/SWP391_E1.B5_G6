package com.carrental.service;

import com.carrental.controller.BookingController.BookingCreateDTO;
import com.carrental.model.*;
import com.carrental.model.Booking.BookingStatus;
import com.carrental.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingDocumentRepository bookingDocumentRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private UserDocumentRepository userDocumentRepository;

    @Autowired
    private ContractService contractService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private UserService userService;

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

        // Validate dates using Vietnam timezone (UTC+7); allow any time today
        if (dto.getStartDate() == null || dto.getEndDate() == null) {
            throw new RuntimeException("Start/end date is required");
        }
        // Past-date validation is handled on front-end; backend only ensures end > start
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new RuntimeException("End date must be after start date");
        }

        // Check if vehicle is available for the requested date range
        // This checks both vehicle status and active bookings/contracts
        if (!vehicleService.isVehicleAvailableForDateRange(
                dto.getVehicleId(), dto.getStartDate(), dto.getEndDate())) {
            throw new RuntimeException("Vehicle is not available for the selected dates");
        }

        // Validate locations
        Location pickupLocation = locationRepository.findById(dto.getPickupLocationId())
                .orElseThrow(() -> new RuntimeException("Pickup location not found"));
        Location returnLocation = locationRepository.findById(dto.getReturnLocationId())
                .orElseThrow(() -> new RuntimeException("Return location not found"));

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
        
        // Sync vehicle status to ensure it reflects current bookings
        vehicleService.syncVehicleStatus(dto.getVehicleId());

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
        Booking savedBooking = bookingRepository.save(booking);

        // Sync vehicle status based on effective availability
        // This ensures the vehicle status matches its actual availability
        vehicleService.syncVehicleStatus(savedBooking.getVehicle().getId());

        // Automatically create contract once booking is approved
        try {
            contractService.createContractFromBooking(savedBooking, staffUser);
        } catch (RuntimeException ex) {
            // If contract already exists, ignore; otherwise propagate
            if (!ex.getMessage().toLowerCase().contains("contract already exists")) {
                throw ex;
            }
        }

        // Send notification to customer requesting deposit payment
        try {
            BigDecimal depositAmount = savedBooking.getVehicle().getDepositAmount();
            notificationService.createBookingApprovalNotification(
                savedBooking.getCustomer().getId(),
                savedBooking.getId(),
                depositAmount
            );
        } catch (Exception e) {
            // Log error but don't fail the approval process
            System.err.println("Failed to send approval notification: " + e.getMessage());
        }

        // Reload with relations to return latest view
        return bookingRepository.findByIdWithRelations(bookingId).orElse(savedBooking);
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

        // Validate rejection reason (must not be empty)
        if (reason == null || reason.trim().isEmpty()) {
            throw new RuntimeException("Vui lòng nhập lý do từ chối đơn đặt xe");
        }

        // Save rejection reason on booking
        booking.setRejectionReason(reason.trim());

        // Update booking status
        booking.setStatus(BookingStatus.REJECTED);
        Booking savedBooking = bookingRepository.save(booking);

        // Sync vehicle status after rejection (vehicle might become available)
        vehicleService.syncVehicleStatus(savedBooking.getVehicle().getId());

        // Send notification to customer about rejection
        try {
            notificationService.createBookingRejectionNotification(
                savedBooking.getCustomer().getId(),
                savedBooking.getId(),
                reason
            );
        } catch (Exception e) {
            // Log error but don't fail the rejection process
            System.err.println("Failed to send rejection notification: " + e.getMessage());
        }

        return savedBooking;
    }

    /**
     * UC08: Cancel booking (Customer or Staff)
     * Can only cancel if no contract has been created yet
     * @param bookingId Booking ID to cancel
     * @param user User performing the action
     * @return Cancelled booking
     */
    public Booking cancelBooking(Long bookingId, User user) {
        System.out.println("=== CANCEL BOOKING START ===");
        System.out.println("Booking ID: " + bookingId);
        System.out.println("User: " + user.getEmail() + " (Role: " + user.getRole() + ")");
        
        // Get the booking from repository to check permissions
        Booking booking = bookingRepository.findByIdWithRelations(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        System.out.println("Current status (enum): " + booking.getStatus());
        System.out.println("Current statusString: " + booking.getStatusString());

        // Validate user has permission to cancel
        if (user.getRole() == User.UserRole.CUSTOMER) {
            if (!booking.getCustomer().getId().equals(user.getId())) {
                throw new RuntimeException("You can only cancel your own bookings");
            }
        }

        // Validate booking status - can only cancel Pending bookings
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException("Only pending bookings can be cancelled");
        }

        System.out.println("Validation passed. Setting status to CANCELLED...");

        // TODO: Check if contract exists - if yes, cannot cancel
        // For now, we assume no contract exists

        // Method 1: Use direct update query (most reliable)
        System.out.println("Using direct update query...");
        int rowsUpdated = bookingRepository.updateBookingStatus(bookingId, "Cancelled");
        System.out.println("Rows updated: " + rowsUpdated);
        
        if (rowsUpdated == 0) {
            throw new RuntimeException("Failed to update booking status");
        }

        // Flush and clear to ensure the update is committed and cache is cleared
        entityManager.flush();
        System.out.println("Flushed to database");
        
        entityManager.clear();
        System.out.println("Cleared EntityManager cache");

        // Reload from database to get updated booking with all relationships
        System.out.println("Reloading from database...");
        Booking updatedBooking = bookingRepository.findByIdWithRelations(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found after update"));
        
        // Sync vehicle status after cancellation (vehicle might become available)
        vehicleService.syncVehicleStatus(updatedBooking.getVehicle().getId());
        
        System.out.println("After reload - ID: " + updatedBooking.getId());
        System.out.println("After reload - enum: " + updatedBooking.getStatus());
        System.out.println("After reload - string: " + updatedBooking.getStatusString());
        System.out.println("=== CANCEL BOOKING END ===");
        
        return updatedBooking;
    }

    /**
     * Cancel booking by ID (simplified version for backward compatibility)
     */
    public Booking cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findByIdWithRelations(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        booking.setStatus(BookingStatus.CANCELLED);
        Booking savedBooking = bookingRepository.save(booking);
        
        // Sync vehicle status after cancellation (vehicle might become available)
        vehicleService.syncVehicleStatus(savedBooking.getVehicle().getId());
        
        return savedBooking;
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

    /**
     * Assign booking to a staff member
     * @param bookingId Booking ID
     * @param staffId Staff user ID to assign
     * @return Updated booking
     */
    public Booking assignBookingToStaff(Long bookingId, Long staffId) {
        Booking booking = bookingRepository.findByIdWithRelations(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // Validate staff exists and is a staff member
        User staff = userService.getUserById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found"));
        
        if (staff.getRole() != User.UserRole.STAFF) {
            throw new RuntimeException("User is not a staff member");
        }

        booking.setAssignedStaff(staff);
        return bookingRepository.save(booking);
    }

    /**
     * Automatically assign booking to staff with least workload
     * Priority: 1. Staff with fewest pending bookings
     *           2. If equal, sort alphabetically by full name
     * @param bookingId Booking ID
     * @return Updated booking with assigned staff
     */
    public Booking autoAssignBookingToStaff(Long bookingId) {
        Booking booking = bookingRepository.findByIdWithRelations(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        List<User> allStaff = userService.findAllStaff();
        
        if (allStaff.isEmpty()) {
            throw new RuntimeException("No staff members available for assignment");
        }

        // Get pending booking count for each staff
        User selectedStaff = allStaff.stream()
                .map(staff -> {
                    long pendingCount = bookingRepository.countPendingBookingsByStaffId(staff.getId());
                    return new java.util.AbstractMap.SimpleEntry<>(staff, pendingCount);
                })
                .min((a, b) -> {
                    // First compare by pending count (ascending)
                    int countCompare = Long.compare(a.getValue(), b.getValue());
                    if (countCompare != 0) {
                        return countCompare;
                    }
                    // If equal, compare by full name alphabetically
                    return a.getKey().getFullName().compareToIgnoreCase(b.getKey().getFullName());
                })
                .map(java.util.Map.Entry::getKey)
                .orElseThrow(() -> new RuntimeException("No staff available"));

        booking.setAssignedStaff(selectedStaff);
        return bookingRepository.save(booking);
    }

    /**
     * Get bookings assigned to a specific staff member
     */
    public List<Booking> getBookingsByAssignedStaff(Long staffId) {
        return bookingRepository.findByAssignedStaffIdWithRelations(staffId);
    }

    /**
     * Get pending bookings assigned to a specific staff member
     */
    public List<Booking> getPendingBookingsByAssignedStaff(Long staffId) {
        return bookingRepository.findPendingBookingsByAssignedStaffIdWithRelations(staffId);
    }

    /**
     * Get bookings by assigned staff and status
     */
    public List<Booking> getBookingsByAssignedStaffAndStatus(Long staffId, BookingStatus status) {
        return bookingRepository.findByAssignedStaffIdAndStatusWithRelations(staffId, status.getValue());
    }

    /**
     * Get unassigned bookings (bookings without assigned staff)
     */
    public List<Booking> getUnassignedBookings() {
        return bookingRepository.findAllWithRelations().stream()
                .filter(b -> b.getAssignedStaff() == null && b.getStatus() == BookingStatus.PENDING)
                .collect(Collectors.toList());
    }
}
