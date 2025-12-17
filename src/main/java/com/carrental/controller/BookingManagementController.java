package com.carrental.controller;

import com.carrental.model.Booking;
import com.carrental.model.Contract;
import com.carrental.model.User;
import com.carrental.model.UserDocument;
import com.carrental.repository.UserRepository;
import com.carrental.service.BookingService;
import com.carrental.service.ContractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Booking Management Controller
 * Handles UC07 (Approve/Reject Booking) and UC10 (Create Contract)
 */
@Controller
@RequestMapping({"/staff/bookings", "/admin/bookings"})
public class BookingManagementController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private ContractService contractService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get current authenticated staff user
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
     * Check if current authenticated user has ADMIN role
     */
    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN"));
    }

    /**
     * UC07: View all bookings (Staff)
     * GET /staff/bookings
     */
    @GetMapping
    public String listBookings(@RequestParam(required = false) String status, Model model) {
        List<Booking> bookings;
        
        if (status != null && !status.isEmpty()) {
            // Filter by status
            try {
                Booking.BookingStatus bookingStatus = Booking.BookingStatus.valueOf(status.toUpperCase());
                bookings = bookingService.getBookingsByStatus(bookingStatus);
            } catch (IllegalArgumentException e) {
                bookings = bookingService.getAllBookings();
            }
        } else {
            bookings = bookingService.getAllBookings();
        }
        
        model.addAttribute("bookings", bookings);
        model.addAttribute("selectedStatus", status);

        // Return view based on role
        if (isAdmin()) {
            return "admin/bookings-manage";
        }
        return "staff/bookings-manage";
    }
    
    /**
     * UC07: View pending bookings for review
     * GET /staff/bookings/pending
     */
    @GetMapping("/pending")
    public String pendingBookings(Model model) {
        List<Booking> pendingBookings = bookingService.getPendingBookings();
        model.addAttribute("bookings", pendingBookings);
        model.addAttribute("selectedStatus", "Pending");

        // Return view based on role
        if (isAdmin()) {
            return "admin/bookings-manage";
        }
        return "staff/bookings-manage";
    }
    
    /**
     * UC07: View booking details with documents
     * GET /staff/bookings/{id}
     */
    @GetMapping("/{id}")
    public String viewBookingDetails(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Booking booking = bookingService.getBookingById(id)
                    .orElseThrow(() -> new RuntimeException("Booking not found"));
            
            // Get booking documents
            List<UserDocument> documents = bookingService.getBookingDocuments(id);
            
            model.addAttribute("booking", booking);
            model.addAttribute("documents", documents);
            
            return "staff/booking-detail";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());

            // Redirect based on role
            if (isAdmin()) {
                return "redirect:/admin/bookings";
            }
            return "redirect:/staff/bookings";
        }
    }
    
    /**
     * UC07 & UC10: Approve booking and create contract
     * POST /staff/bookings/{id}/approve
     *
     * Workflow:
     * 1. Staff reviews documents
     * 2. Staff approves booking
     * 3. System automatically creates contract with PENDING_PAYMENT status
     * 4. Contract is sent to customer for review and deposit payment
     */
    @PostMapping("/{id}/approve")
    public String approveBooking(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            User currentStaff = getCurrentUser();

            // Approve booking; service will auto-create contract if needed
            Booking booking = bookingService.approveBooking(id, currentStaff);

            // Fetch created/ existing contract for redirect
            Contract contract = contractService.getContractByBookingId(booking.getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng sau khi duyệt"));

            redirectAttributes.addFlashAttribute("successMessage",
                "Đã duyệt đơn đặt xe #" + booking.getId() + " và tạo hợp đồng #" + contract.getContractNumber() +
                " thành công. Hợp đồng đã được gửi cho khách hàng để xác nhận và thanh toán cọc.");
            
            // Điều hướng tới trang hợp đồng vừa tạo để nhân viên xem/xác nhận thông tin đã được tự động điền
            return "redirect:/staff/contracts/" + contract.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Lỗi khi duyệt đơn: " + e.getMessage());
        }

        return "redirect:/staff/bookings/" + id;
    }
    
    /**
     * UC07: Reject booking
     * POST /staff/bookings/{id}/reject
     */
    @PostMapping("/{id}/reject")
    public String rejectBooking(@PathVariable Long id, 
                               @RequestParam(required = false) String reason,
                               RedirectAttributes redirectAttributes) {
        try {
            User currentStaff = getCurrentUser();
            
            Booking booking = bookingService.rejectBooking(id, currentStaff, reason);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Đã từ chối đơn đặt xe #" + booking.getId());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Lỗi khi từ chối đơn: " + e.getMessage());
        }

        // Redirect based on role
        if (isAdmin()) {
            return "redirect:/admin/bookings/" + id;
        }
        return "redirect:/staff/bookings/" + id;
    }
}

