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
            User user = getUserByEmail(userDetails.getUsername());
            List<Notification> notifications = notificationService.getNotificationsByUserId(user.getId());
            long unreadCount = notificationService.getUnreadCount(user.getId());

            model.addAttribute("notifications", notifications);
            model.addAttribute("unreadCount", unreadCount);
            return getNotificationView(user.getRole().toString());
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Lỗi khi tải thông báo: " + e.getMessage());
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

