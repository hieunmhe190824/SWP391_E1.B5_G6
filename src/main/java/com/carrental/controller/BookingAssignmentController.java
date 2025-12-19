package com.carrental.controller;

import com.carrental.model.Booking;
import com.carrental.model.User;
import com.carrental.repository.UserRepository;
import com.carrental.service.BookingService;
import com.carrental.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Booking Assignment Controller (Admin only)
 * Handles assignment of bookings to staff members
 */
@Controller
@RequestMapping("/admin/bookings/assign")
public class BookingAssignmentController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get current authenticated admin user
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
     * View unassigned bookings and assignment page
     * GET /admin/bookings/assign
     */
    @GetMapping
    public String assignmentPage(Model model) {
        // Get all unassigned pending bookings
        List<Booking> unassignedBookings = bookingService.getUnassignedBookings();
        
        // Get all staff members
        List<User> staffMembers = userService.findAllStaff();
        
        // Get pending booking count for each staff (for display)
        List<StaffWorkload> staffWorkloads = staffMembers.stream()
                .map(staff -> {
                    long pendingCount = bookingService.getPendingBookingsByAssignedStaff(staff.getId()).size();
                    return new StaffWorkload(staff, pendingCount);
                })
                .sorted((a, b) -> {
                    // Sort by pending count (ascending), then by name (alphabetically)
                    int countCompare = Long.compare(a.getPendingCount(), b.getPendingCount());
                    if (countCompare != 0) {
                        return countCompare;
                    }
                    return a.getStaff().getFullName().compareToIgnoreCase(b.getStaff().getFullName());
                })
                .toList();

        model.addAttribute("unassignedBookings", unassignedBookings);
        model.addAttribute("staffWorkloads", staffWorkloads);
        model.addAttribute("staffMembers", staffMembers);
        
        return "admin/booking-assignment";
    }

    /**
     * Assign a booking to a specific staff member
     * POST /admin/bookings/assign/{bookingId}
     */
    @PostMapping("/{bookingId}")
    public String assignBooking(@PathVariable Long bookingId,
                               @RequestParam Long staffId,
                               RedirectAttributes redirectAttributes) {
        try {
            Booking booking = bookingService.assignBookingToStaff(bookingId, staffId);
            User staff = userService.getUserById(staffId)
                    .orElseThrow(() -> new RuntimeException("Staff not found"));
            
            redirectAttributes.addFlashAttribute("successMessage",
                "Đã phân công đơn đặt #" + booking.getId() + " cho nhân viên " + staff.getFullName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Lỗi khi phân công: " + e.getMessage());
        }
        
        return "redirect:/admin/bookings/assign";
    }

    /**
     * Auto-assign a booking to staff with least workload
     * POST /admin/bookings/assign/{bookingId}/auto
     */
    @PostMapping("/{bookingId}/auto")
    public String autoAssignBooking(@PathVariable Long bookingId,
                                   RedirectAttributes redirectAttributes) {
        try {
            Booking booking = bookingService.autoAssignBookingToStaff(bookingId);
            User staff = booking.getAssignedStaff();
            
            redirectAttributes.addFlashAttribute("successMessage",
                "Đã tự động phân công đơn đặt #" + booking.getId() + " cho nhân viên " + staff.getFullName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Lỗi khi tự động phân công: " + e.getMessage());
        }
        
        return "redirect:/admin/bookings/assign";
    }

    /**
     * Auto-assign all unassigned bookings
     * POST /admin/bookings/assign/auto-all
     */
    @PostMapping("/auto-all")
    public String autoAssignAll(RedirectAttributes redirectAttributes) {
        try {
            List<Booking> unassignedBookings = bookingService.getUnassignedBookings();
            int assignedCount = 0;
            
            for (Booking booking : unassignedBookings) {
                try {
                    bookingService.autoAssignBookingToStaff(booking.getId());
                    assignedCount++;
                } catch (Exception e) {
                    // Continue with other bookings even if one fails
                    System.err.println("Failed to assign booking #" + booking.getId() + ": " + e.getMessage());
                }
            }
            
            redirectAttributes.addFlashAttribute("successMessage",
                "Đã tự động phân công " + assignedCount + " đơn đặt cho nhân viên");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Lỗi khi tự động phân công: " + e.getMessage());
        }
        
        return "redirect:/admin/bookings/assign";
    }

    /**
     * Inner class to hold staff workload information
     */
    public static class StaffWorkload {
        private User staff;
        private long pendingCount;

        public StaffWorkload(User staff, long pendingCount) {
            this.staff = staff;
            this.pendingCount = pendingCount;
        }

        public User getStaff() {
            return staff;
        }

        public long getPendingCount() {
            return pendingCount;
        }
    }
}

