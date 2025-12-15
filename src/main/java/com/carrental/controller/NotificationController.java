package com.carrental.controller;

import com.carrental.model.Notification;
import com.carrental.model.User;
import com.carrental.repository.UserRepository;
import com.carrental.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    /**
     * UC23: Show all notifications for current user
     */
    @GetMapping
    public String showNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        try {
            System.out.println("=== NOTIFICATIONS DEBUG START ===");
            
            // Check if user is authenticated
            if (userDetails == null) {
                System.out.println("ERROR: userDetails is null");
                return "redirect:/auth/login";
            }
            System.out.println("User email: " + userDetails.getUsername());
            
            User user = getUserByEmail(userDetails.getUsername());
            System.out.println("User found: " + user.getId() + " - " + user.getFullName());
            
            // Check if user exists and has a role
            if (user == null) {
                System.out.println("ERROR: user is null");
                model.addAttribute("errorMessage", "Không tìm thấy thông tin người dùng");
                return "error";
            }
            
            if (user.getRole() == null) {
                System.out.println("ERROR: user role is null");
                model.addAttribute("errorMessage", "Người dùng không có quyền hạn được gán");
                return "error";
            }
            System.out.println("User role: " + user.getRole());
            
            System.out.println("Fetching notifications for user: " + user.getId());
            List<Notification> notifications = notificationService.getNotificationsByUserId(user.getId());
            System.out.println("Found " + notifications.size() + " notifications");
            
            long unreadCount = notificationService.getUnreadCount(user.getId());
            System.out.println("Unread count: " + unreadCount);

            model.addAttribute("notifications", notifications);
            model.addAttribute("unreadCount", unreadCount);
            // Provide unread count for header badge on this page
            model.addAttribute("unreadNotificationCount", unreadCount);
            
            String viewName = getNotificationView(user.getRole().toString());
            System.out.println("Returning view: " + viewName);
            System.out.println("=== NOTIFICATIONS DEBUG END ===");
            
            return viewName;
        } catch (Exception e) {
            // Log the full exception for debugging
            System.out.println("=== EXCEPTION CAUGHT ===");
            System.out.println("Exception type: " + e.getClass().getName());
            System.out.println("Exception message: " + e.getMessage());
            e.printStackTrace();
            System.out.println("=== END EXCEPTION ===");
            
            model.addAttribute("errorMessage", "Lỗi khi tải thông báo: " + e.getMessage());
            model.addAttribute("errorDetails", e.getClass().getName());
            return "error";
        }
    }

    /**
     * UC23: Mark notification as read (GET for click, POST for form)
     */
    @GetMapping("/{notificationId}/read")
    public String markAsReadGet(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        return markAsRead(notificationId, userDetails, redirectAttributes);
    }

    @PostMapping("/{notificationId}/read")
    public String markAsRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            notificationService.markAsRead(notificationId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã đánh dấu đã đọc");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/notifications";
    }

    /**
     * UC23: Mark all notifications as read
     */
    @PostMapping("/read-all")
    public String markAllAsRead(
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            User user = getUserByEmail(userDetails.getUsername());
            notificationService.markAllAsRead(user.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Đã đánh dấu tất cả là đã đọc");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/notifications";
    }

    /**
     * Get notification view based on user role
     */
    private String getNotificationView(String role) {
        switch (role.toUpperCase()) {
            case "CUSTOMER":
                return "customer/notifications";
            case "STAFF":
                return "staff/notifications";
            case "ADMIN":
                return "admin/notifications";
            default:
                return "notifications";
        }
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }
}

