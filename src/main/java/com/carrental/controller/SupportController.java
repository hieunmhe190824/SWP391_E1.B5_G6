package com.carrental.controller;

import com.carrental.model.SupportMessage;
import com.carrental.model.SupportTicket;
import com.carrental.model.SupportTicket.Category;
import com.carrental.model.SupportTicket.TicketStatus;
import com.carrental.model.User;
import com.carrental.service.SupportService;
import com.carrental.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

/**
 * Support Controller
 * Handles UC19 (Submit Support Request), UC20 (Manage Support Tickets), UC21 (Rate Support)
 */
@Controller
public class SupportController {

    @Autowired
    private SupportService supportService;
    
    @Autowired
    private UserService userService;

    /**
     * Get current authenticated user
     */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            String email = auth.getName();
            User user = userService.findByEmail(email);
            if (user == null) {
                throw new RuntimeException("User not found");
            }
            return user;
        }
        return null;
    }

    // ==================== UNIVERSAL ENDPOINT ====================
    
    /**
     * Universal support tickets endpoint - redirects based on role
     * GET /support/tickets
     */
    @GetMapping("/support/tickets")
    public String supportTickets() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/auth/login";
        }
        
        // Check if user is staff or admin
        String role = currentUser.getRole().name();
        if (role.equals("STAFF") || role.equals("ADMIN")) {
            return "redirect:/staff/support/tickets";
        } else {
            return "redirect:/customer/support/tickets";
        }
    }
    
    // ==================== CUSTOMER ENDPOINTS ====================
    
    /**
     * UC19: Show create ticket form
     * GET /customer/support/create
     */
    @GetMapping("/customer/support/create")
    public String createTicketPage(Model model) {
        model.addAttribute("ticket", new SupportTicket());
        model.addAttribute("categories", Category.values());
        return "customer/support-create";
    }

    /**
     * UC19: Submit new support ticket
     * POST /customer/support/create
     */
    @PostMapping("/customer/support/create")
    public String createTicket(@ModelAttribute SupportTicket ticket,
                              RedirectAttributes redirectAttributes) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser != null) {
                ticket.setCustomer(currentUser);
            }
            
            supportService.createTicket(ticket);
            
            redirectAttributes.addFlashAttribute("success", 
                "Yêu cầu hỗ trợ đã được gửi thành công! Mã ticket: " + ticket.getTicketNumber());
            return "redirect:/customer/support/tickets";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", 
                "Không thể tạo yêu cầu hỗ trợ: " + e.getMessage());
            return "redirect:/customer/support/create";
        }
    }

    /**
     * UC19: List customer's tickets
     * GET /customer/support/tickets
     */
    @GetMapping("/customer/support/tickets")
    public String listCustomerTickets(@RequestParam(required = false) String status,
                                     Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        List<SupportTicket> tickets;
        if (status != null && !status.isEmpty()) {
            // Filter by status
            TicketStatus ticketStatus = TicketStatus.valueOf(status);
            tickets = supportService.getTicketsByStatus(ticketStatus).stream()
                    .filter(t -> t.getCustomer() != null && 
                                t.getCustomer().getId().equals(currentUser.getId()))
                    .toList();
        } else {
            tickets = supportService.getTicketsByCustomer(currentUser.getId());
        }
        
        model.addAttribute("tickets", tickets);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", TicketStatus.values());
        return "customer/support-tickets";
    }

    /**
     * View ticket details with messages
     * GET /customer/support/tickets/{id}
     */
    @GetMapping("/customer/support/tickets/{id}")
    public String viewCustomerTicket(@PathVariable Long id,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        Optional<SupportTicket> ticketOpt = supportService.getTicketById(id);
        if (ticketOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy ticket");
            return "redirect:/customer/support/tickets";
        }
        
        SupportTicket ticket = ticketOpt.get();
        
        // Verify ticket belongs to current user
        if (ticket.getCustomer() == null || 
            !ticket.getCustomer().getId().equals(currentUser.getId())) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền xem ticket này");
            return "redirect:/customer/support/tickets";
        }
        
        List<SupportMessage> messages = supportService.getMessages(id);
        Optional<SupportMessage> rating = supportService.getTicketRating(id);
        
        model.addAttribute("ticket", ticket);
        model.addAttribute("messages", messages);
        model.addAttribute("rating", rating.orElse(null));
        model.addAttribute("canRate", 
            (ticket.getStatus() == TicketStatus.RESOLVED || 
             ticket.getStatus() == TicketStatus.CLOSED) && 
            rating.isEmpty());
        
        return "customer/support-detail";
    }

    /**
     * Add message to ticket (customer reply)
     * POST /customer/support/tickets/{id}/message
     */
    @PostMapping("/customer/support/tickets/{id}/message")
    public String addCustomerMessage(@PathVariable Long id,
                                    @RequestParam String messageText,
                                    RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        try {
            // Verify ticket belongs to current user
            Optional<SupportTicket> ticketOpt = supportService.getTicketById(id);
            if (ticketOpt.isEmpty() || 
                ticketOpt.get().getCustomer() == null ||
                !ticketOpt.get().getCustomer().getId().equals(currentUser.getId())) {
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền trả lời ticket này");
                return "redirect:/customer/support/tickets";
            }
            
            supportService.addMessage(id, currentUser.getId(), messageText);
            redirectAttributes.addFlashAttribute("success", "Tin nhắn đã được gửi");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không thể gửi tin nhắn: " + e.getMessage());
        }
        
        return "redirect:/customer/support/tickets/" + id;
    }

    /**
     * UC21: Rate support ticket
     * POST /customer/support/tickets/{id}/rate
     */
    @PostMapping("/customer/support/tickets/{id}/rate")
    public String rateTicket(@PathVariable Long id,
                            @RequestParam Integer rating,
                            @RequestParam(required = false) String feedback,
                            RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        try {
            supportService.rateSupport(id, currentUser.getId(), rating, feedback);
            redirectAttributes.addFlashAttribute("success", "Cảm ơn bạn đã đánh giá!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không thể đánh giá: " + e.getMessage());
        }
        
        return "redirect:/customer/support/tickets/" + id;
    }

    // ==================== STAFF/ADMIN ENDPOINTS ====================
    
    /**
     * UC20: List all support tickets (staff/admin)
     * GET /staff/support/tickets
     */
    @GetMapping("/staff/support/tickets")
    public String listAllTickets(@RequestParam(required = false) String status,
                                 Model model) {
        List<SupportTicket> tickets;
        
        if (status != null && !status.isEmpty()) {
            TicketStatus ticketStatus = TicketStatus.valueOf(status);
            tickets = supportService.getTicketsByStatus(ticketStatus);
        } else {
            tickets = supportService.getAllTickets();
        }
        
        model.addAttribute("tickets", tickets);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", TicketStatus.values());
        return "staff/support-tickets-manage";
    }

    /**
     * UC20: View ticket details (staff/admin)
     * GET /staff/support/tickets/{id}
     */
    @GetMapping("/staff/support/tickets/{id}")
    public String viewStaffTicket(@PathVariable Long id,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        Optional<SupportTicket> ticketOpt = supportService.getTicketById(id);
        if (ticketOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy ticket");
            return "redirect:/staff/support/tickets";
        }
        
        SupportTicket ticket = ticketOpt.get();
        List<SupportMessage> messages = supportService.getMessages(id);
        Optional<SupportMessage> rating = supportService.getTicketRating(id);
        
        // Get all staff for assignment dropdown
        List<User> staffList = userService.findAllStaff();
        
        model.addAttribute("ticket", ticket);
        model.addAttribute("messages", messages);
        model.addAttribute("rating", rating.orElse(null));
        model.addAttribute("staffList", staffList);
        model.addAttribute("statuses", TicketStatus.values());
        
        return "staff/support-ticket-detail";
    }

    /**
     * UC20: Reply to ticket (staff)
     * POST /staff/support/tickets/{id}/reply
     */
    @PostMapping("/staff/support/tickets/{id}/reply")
    public String replyToTicket(@PathVariable Long id,
                               @RequestParam String messageText,
                               RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        try {
            supportService.addMessage(id, currentUser.getId(), messageText);
            redirectAttributes.addFlashAttribute("success", "Đã gửi phản hồi");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không thể gửi phản hồi: " + e.getMessage());
        }
        
        return "redirect:/staff/support/tickets/" + id;
    }

    /**
     * UC20: Assign ticket to staff
     * POST /staff/support/tickets/{id}/assign
     */
    @PostMapping("/staff/support/tickets/{id}/assign")
    public String assignTicket(@PathVariable Long id,
                              @RequestParam Long staffId,
                              RedirectAttributes redirectAttributes) {
        try {
            supportService.assignTicket(id, staffId);
            redirectAttributes.addFlashAttribute("success", "Đã phân công ticket");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không thể phân công: " + e.getMessage());
        }
        
        return "redirect:/staff/support/tickets/" + id;
    }

    /**
     * UC20: Update ticket status
     * POST /staff/support/tickets/{id}/status
     */
    @PostMapping("/staff/support/tickets/{id}/status")
    public String updateTicketStatus(@PathVariable Long id,
                                    @RequestParam String status,
                                    RedirectAttributes redirectAttributes) {
        try {
            TicketStatus ticketStatus = TicketStatus.valueOf(status);
            supportService.updateTicketStatus(id, ticketStatus);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật trạng thái");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không thể cập nhật: " + e.getMessage());
        }
        
        return "redirect:/staff/support/tickets/" + id;
    }
}
