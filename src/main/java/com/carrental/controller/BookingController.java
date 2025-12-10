package com.carrental.controller;

import com.carrental.model.Booking;
import com.carrental.model.Location;
import com.carrental.model.User;
import com.carrental.model.UserDocument;
import com.carrental.model.Vehicle;
import com.carrental.repository.UserRepository;
import com.carrental.service.BookingService;
import com.carrental.service.LocationService;
import com.carrental.service.UserDocumentService;
import com.carrental.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Customer Booking Controller
 * Handles UC06 (Create Booking), UC08 (Cancel Booking), UC09 (View Booking History)
 */
@Controller
@RequestMapping("/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private LocationService locationService;

    @Autowired
    private UserDocumentService userDocumentService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get current authenticated user
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * UC06: Show booking creation page
     * GET /bookings/create?vehicleId={id}
     */
    @GetMapping("/create")
    public String createBookingPage(@RequestParam(required = false) Long vehicleId,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        // Validate vehicleId parameter
        if (vehicleId == null) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Vui lòng chọn xe để đặt.");
            return "redirect:/vehicles";
        }

        User currentUser = getCurrentUser();

        // Get vehicle details
        Vehicle vehicle = vehicleService.getVehicleById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        // Get all locations for pickup/return selection
        List<Location> locations = locationService.getAllLocations();

        // Get user's approved documents
        List<UserDocument> userDocuments = userDocumentService.getDocumentsByUser(currentUser);

        model.addAttribute("vehicle", vehicle);
        model.addAttribute("locations", locations);
        model.addAttribute("userDocuments", userDocuments);
        model.addAttribute("bookingDTO", new BookingCreateDTO());

        return "customer/booking-create";
    }

    /**
     * UC06: Create new booking
     * POST /bookings/create
     */
    @PostMapping("/create")
    public String createBooking(@ModelAttribute BookingCreateDTO bookingDTO,
                               RedirectAttributes redirectAttributes) {
        try {
            // Log the incoming data for debugging
            System.out.println("=== BOOKING CREATE DEBUG ===");
            System.out.println("Vehicle ID: " + bookingDTO.getVehicleId());
            System.out.println("Pickup Location ID: " + bookingDTO.getPickupLocationId());
            System.out.println("Return Location ID: " + bookingDTO.getReturnLocationId());
            System.out.println("Start Date: " + bookingDTO.getStartDate());
            System.out.println("End Date: " + bookingDTO.getEndDate());
            System.out.println("Document IDs: " + bookingDTO.getDocumentIds());

            User currentUser = getCurrentUser();
            System.out.println("Current User: " + currentUser.getEmail());

            // Create booking
            Booking booking = bookingService.createBooking(bookingDTO, currentUser);
            System.out.println("Booking created with ID: " + booking.getId());

            // Build detailed success message
            String vehicleName = booking.getVehicle().getModel().getBrand().getBrandName() +
                               " " + booking.getVehicle().getModel().getModelName();
            String startDate = booking.getStartDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            String endDate = booking.getEndDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

            String successMessage = String.format(
                "✓ Đặt xe thành công! Mã đơn: #%d | Xe: %s | Từ %s đến %s | Vui lòng chờ nhân viên xác nhận.",
                booking.getId(),
                vehicleName,
                startDate,
                endDate
            );

            redirectAttributes.addFlashAttribute("successMessage", successMessage);
            System.out.println("Success message set: " + successMessage);
            System.out.println("Redirecting to: /bookings/my-bookings");

            return "redirect:/bookings/my-bookings";
        } catch (Exception e) {
            // Log the full exception
            System.err.println("=== BOOKING CREATE ERROR ===");
            System.err.println("Error message: " + e.getMessage());
            e.printStackTrace();

            redirectAttributes.addFlashAttribute("errorMessage",
                "Lỗi khi đặt xe: " + e.getMessage());
            return "redirect:/bookings/create?vehicleId=" + bookingDTO.getVehicleId();
        }
    }

    /**
     * UC09: View booking history
     * GET /bookings/my-bookings
     */
    @GetMapping("/my-bookings")
    public String myBookings(@RequestParam(required = false) String status, Model model) {
        User currentUser = getCurrentUser();

        List<Booking> bookings;
        if (status != null && !status.isEmpty()) {
            // Filter by status
            try {
                Booking.BookingStatus bookingStatus = Booking.BookingStatus.valueOf(status.toUpperCase());
                bookings = bookingService.getBookingsByCustomer(currentUser.getId()).stream()
                        .filter(b -> b.getStatus() == bookingStatus)
                        .toList();
            } catch (IllegalArgumentException e) {
                bookings = bookingService.getBookingsByCustomer(currentUser.getId());
            }
        } else {
            bookings = bookingService.getBookingsByCustomer(currentUser.getId());
        }

        model.addAttribute("bookings", bookings);
        model.addAttribute("selectedStatus", status);

        return "customer/my-bookings";
    }

    /**
     * View booking details
     * GET /bookings/{id}
     */
    @GetMapping("/{id}")
    public String viewBookingDetails(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            User currentUser = getCurrentUser();

            Booking booking = bookingService.getBookingById(id)
                    .orElseThrow(() -> new RuntimeException("Booking not found"));

            // Verify user owns this booking
            if (!booking.getCustomer().getId().equals(currentUser.getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền xem đơn đặt này");
                return "redirect:/bookings/my-bookings";
            }

            // Get booking documents
            List<UserDocument> documents = bookingService.getBookingDocuments(id);

            model.addAttribute("booking", booking);
            model.addAttribute("documents", documents);

            return "customer/booking-detail";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/bookings/my-bookings";
        }
    }

    /**
     * UC08: Cancel booking
     * POST /bookings/{id}/cancel
     */
    @PostMapping("/{id}/cancel")
    public String cancelBooking(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        System.out.println("=== CONTROLLER: Cancel Booking Request ===");
        System.out.println("Booking ID from path: " + id);
        
        try {
            User currentUser = getCurrentUser();
            System.out.println("Current user: " + currentUser.getEmail());

            Booking booking = bookingService.cancelBooking(id, currentUser);
            
            System.out.println("=== CONTROLLER: After Service Call ===");
            System.out.println("Returned booking ID: " + booking.getId());
            System.out.println("Returned booking status enum: " + booking.getStatus());
            System.out.println("Returned booking status string: " + booking.getStatusString());

            redirectAttributes.addFlashAttribute("successMessage",
                "Đã hủy đơn đặt xe #" + booking.getId() + " thành công");
            
            System.out.println("=== CONTROLLER: Redirecting to my-bookings ===");
        } catch (Exception e) {
            System.err.println("=== CONTROLLER: ERROR ===");
            System.err.println("Error message: " + e.getMessage());
            e.printStackTrace();
            
            redirectAttributes.addFlashAttribute("errorMessage",
                "Lỗi khi hủy đơn: " + e.getMessage());
        }

        return "redirect:/bookings/my-bookings";
    }

    /**
     * Handle MethodArgumentTypeMismatchException
     * This catches errors when request parameters cannot be converted to the expected type
     * For example: vehicleId=null cannot be converted to Long
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public String handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                    RedirectAttributes redirectAttributes) {
        String paramName = ex.getName();

        // Check if it's a vehicleId parameter issue
        if ("vehicleId".equals(paramName)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Vui lòng chọn xe để đặt. ID xe không hợp lệ.");
            return "redirect:/vehicles";
        }

        // Generic error message for other parameter issues
        redirectAttributes.addFlashAttribute("errorMessage",
            "Tham số không hợp lệ: " + paramName);
        return "redirect:/vehicles";
    }

    // ===== INNER DTO CLASSES =====

    /**
     * DTO for creating a new booking
     * Used in UC06: Create Booking
     */
    public static class BookingCreateDTO {

        private Long vehicleId;
        private Long pickupLocationId;
        private Long returnLocationId;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private List<Long> documentIds; // IDs of user documents to attach

        // Constructors
        public BookingCreateDTO() {
        }

        public BookingCreateDTO(Long vehicleId, Long pickupLocationId, Long returnLocationId,
                               LocalDateTime startDate, LocalDateTime endDate, List<Long> documentIds) {
            this.vehicleId = vehicleId;
            this.pickupLocationId = pickupLocationId;
            this.returnLocationId = returnLocationId;
            this.startDate = startDate;
            this.endDate = endDate;
            this.documentIds = documentIds;
        }

        // Getters and Setters
        public Long getVehicleId() {
            return vehicleId;
        }

        public void setVehicleId(Long vehicleId) {
            this.vehicleId = vehicleId;
        }

        public Long getPickupLocationId() {
            return pickupLocationId;
        }

        public void setPickupLocationId(Long pickupLocationId) {
            this.pickupLocationId = pickupLocationId;
        }

        public Long getReturnLocationId() {
            return returnLocationId;
        }

        public void setReturnLocationId(Long returnLocationId) {
            this.returnLocationId = returnLocationId;
        }

        public LocalDateTime getStartDate() {
            return startDate;
        }

        public void setStartDate(LocalDateTime startDate) {
            this.startDate = startDate;
        }

        public LocalDateTime getEndDate() {
            return endDate;
        }

        public void setEndDate(LocalDateTime endDate) {
            this.endDate = endDate;
        }

        public List<Long> getDocumentIds() {
            return documentIds;
        }

        public void setDocumentIds(List<Long> documentIds) {
            this.documentIds = documentIds;
        }
    }
}
