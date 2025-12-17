package com.carrental.controller;

import com.carrental.model.User;
import com.carrental.model.Booking;
import com.carrental.service.BookingService;
import com.carrental.service.SupportService;
import com.carrental.service.UserService;
import com.carrental.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Dashboard Controller
 * Xử lý dashboard và chức năng chung cho cả Admin và Staff:
 * 
 * Admin routes (/admin/**):
 * - Dashboard với thống kê tổng quan
 * - Quản lý users
 * - Profile management
 * 
 * Staff routes (/staff/**):
 * - Dashboard với bookings overview
 * - Booking management
 * - Support ticket management
 * - Profile management
 */
@Controller
public class DashboardController {

    @Autowired
    private UserService userService;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private BookingService bookingService;
    
    @Autowired
    private SupportService supportService;
    
    // ========================================
    // ADMIN ROUTES
    // ========================================

    /**
     * Admin Dashboard - Hiển thị thống kê tổng quan
     * GET /admin/dashboard
     */
    @GetMapping("/admin/dashboard")
    public String adminDashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        // Load statistics cho dashboard
        try {
            var bookings = bookingService.getAllBookings();
            model.addAttribute("totalUsers", userService.getAllUsers().size());
            model.addAttribute("totalVehicles", vehicleService.getAllVehicles().size());
            model.addAttribute("totalBookings", bookings.size());
            model.addAttribute("bookings", bookings);
            addBookingStatusCounts(model, bookings);
        } catch (Exception e) {
            // Nếu có lỗi, set giá trị mặc định
            System.err.println("Error loading dashboard data: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("totalUsers", 0);
            model.addAttribute("totalVehicles", 0);
            model.addAttribute("totalBookings", 0);
            model.addAttribute("bookings", java.util.Collections.emptyList());
            // Set default counts
            model.addAttribute("pendingCount", 0);
            model.addAttribute("approvedCount", 0);
            model.addAttribute("rejectedCount", 0);
            model.addAttribute("cancelledCount", 0);
        }

        // Add current user for header
        if (userDetails != null) {
            User currentUser = userService.findByUsername(userDetails.getUsername());
            model.addAttribute("currentUser", currentUser);
        }

        return "admin/dashboard";
    }

    /**
     * Admin - Quản lý tất cả người dùng
     * GET /admin/users
     */
    @GetMapping("/admin/users")
    public String manageUsers(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails != null) {
            User currentUser = userService.findByUsername(userDetails.getUsername());
            model.addAttribute("currentUser", currentUser);
        }
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("activeFilter", "all");
        return "admin/users";
    }

    /**
     * Admin - Chỉ xem khách hàng
     * GET /admin/users/customers
     */
    @GetMapping("/admin/users/customers")
    public String manageCustomerUsers(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails != null) {
            User currentUser = userService.findByUsername(userDetails.getUsername());
            model.addAttribute("currentUser", currentUser);
        }
        model.addAttribute("users", userService.getAllCustomers());
        model.addAttribute("activeFilter", "customers");
        return "admin/users";
    }

    /**
     * Admin - Chỉ xem nhân viên & admin
     * GET /admin/users/staff
     */
    @GetMapping("/admin/users/staff")
    public String manageStaffUsers(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails != null) {
            User currentUser = userService.findByUsername(userDetails.getUsername());
            model.addAttribute("currentUser", currentUser);
        }
        model.addAttribute("users", userService.findAllStaff());
        model.addAttribute("activeFilter", "staff");
        return "admin/users";
    }

    /**
     * Admin - Cập nhật trạng thái user (ACTIVE/INACTIVE)
     * POST /admin/users/{id}/status
     */
    @PostMapping("/admin/users/{id}/status")
    public String updateUserStatus(
            @PathVariable Long id,
            @RequestParam String status,
            RedirectAttributes redirectAttributes) {
        try {
            User.UserStatus newStatus = User.UserStatus.valueOf(status);
            userService.updateUserStatus(id, newStatus);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái người dùng thành công");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Trạng thái không hợp lệ");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    /**
     * Admin - Xem chi tiết 1 user bất kỳ
     * GET /admin/users/{id}
     */
    @GetMapping("/admin/users/{id}")
    public String viewUserDetail(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            // current logged-in admin
            if (userDetails != null) {
                User currentUser = userService.findByUsername(userDetails.getUsername());
                model.addAttribute("currentUser", currentUser);
            }

            User targetUser = userService.getUserById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
            model.addAttribute("user", targetUser);
            return "admin/user-detail";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể tải thông tin người dùng: " + e.getMessage());
            return "redirect:/admin/users";
        }
    }

    /**
     * Admin - Form chỉnh sửa thông tin user
     * GET /admin/users/{id}/edit
     */
    @GetMapping("/admin/users/{id}/edit")
    public String editUserForm(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            if (userDetails != null) {
                User currentUser = userService.findByUsername(userDetails.getUsername());
                model.addAttribute("currentUser", currentUser);
            }

            User targetUser = userService.getUserById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
            model.addAttribute("user", targetUser);
            model.addAttribute("allRoles", User.UserRole.values());
            model.addAttribute("allStatuses", User.UserStatus.values());
            return "admin/user-edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể tải form chỉnh sửa: " + e.getMessage());
            return "redirect:/admin/users";
        }
    }

    /**
     * Admin - Xử lý cập nhật thông tin & role của user
     * POST /admin/users/{id}/edit
     */
    @PostMapping("/admin/users/{id}/edit")
    public String updateUser(
            @PathVariable Long id,
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam(required = false) String address,
            @RequestParam String role,
            @RequestParam String status,
            RedirectAttributes redirectAttributes) {
        try {
            User user = userService.getUserById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            // Validate email format
            if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                redirectAttributes.addFlashAttribute("errorMessage", "Email không hợp lệ");
                return "redirect:/admin/users/" + id + "/edit";
            }

            // Check if email is already taken by another user
            User existingUser = userService.findByEmail(email);
            if (existingUser != null && !existingUser.getId().equals(user.getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Email đã được sử dụng bởi tài khoản khác");
                return "redirect:/admin/users/" + id + "/edit";
            }

            // Clean phone number
            String phoneDigits = phone.replaceAll("[\\s\\-]", "");
            if (!phoneDigits.matches("^[0-9]{10,11}$")) {
                redirectAttributes.addFlashAttribute("errorMessage", "Số điện thoại phải có 10-11 chữ số");
                return "redirect:/admin/users/" + id + "/edit";
            }

            // Parse role & status
            User.UserRole newRole = User.UserRole.valueOf(role);
            User.UserStatus newStatus = User.UserStatus.valueOf(status);

            // Update fields
            user.setFullName(fullName);
            user.setEmail(email);
            user.setPhone(phoneDigits);
            user.setAddress(address);
            user.setRole(newRole);
            user.setStatus(newStatus);

            userService.updateUser(user);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật người dùng thành công");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Dữ liệu không hợp lệ: " + e.getMessage());
            return "redirect:/admin/users/" + id + "/edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/admin/users/" + id + "/edit";
        }

        return "redirect:/admin/users/" + id;
    }

    /**
     * Admin Profile - Xem profile
     * GET /admin/profile
     */
    @GetMapping("/admin/profile")
    public String adminProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("currentUser", user);
        model.addAttribute("pageTitle", "Hồ sơ của tôi");
        return "admin/profile";
    }

    /**
     * Admin - Edit Profile form
     * GET /admin/profile/edit
     */
    @GetMapping("/admin/profile/edit")
    public String adminEditProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("currentUser", user);
        return "admin/profile-edit";
    }

    /**
     * Admin - Update Profile
     * POST /admin/profile/update
     */
    @PostMapping("/admin/profile/update")
    public String adminUpdateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam(required = false) String address,
            RedirectAttributes redirectAttributes) {
        
        User user = userService.findByUsername(userDetails.getUsername());
        
        try {
            // Validate email format
            if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                redirectAttributes.addFlashAttribute("error", "Email không hợp lệ");
                return "redirect:/admin/profile/edit";
            }

            // Check if email is already taken by another user
            User existingUser = userService.findByEmail(email);
            if (existingUser != null && !existingUser.getId().equals(user.getId())) {
                redirectAttributes.addFlashAttribute("error", "Email đã được sử dụng bởi tài khoản khác");
                return "redirect:/admin/profile/edit";
            }

            // Clean phone number
            String phoneDigits = phone.replaceAll("[\\s\\-]", "");
            if (!phoneDigits.matches("^[0-9]{10,11}$")) {
                redirectAttributes.addFlashAttribute("error", "Số điện thoại phải có 10-11 chữ số");
                return "redirect:/admin/profile/edit";
            }

            // Update user info including email
            user.setEmail(email);
            user.setFullName(fullName);
            user.setPhone(phoneDigits);
            user.setAddress(address);
            userService.updateUser(user);
            
            redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/admin/profile/edit";
        }

        return "redirect:/admin/profile";
    }

    /**
     * Admin - Change Password
     * POST /admin/profile/change-password
     */
    @PostMapping("/admin/profile/change-password")
    public String adminChangePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes) {
        
        User user = userService.findByUsername(userDetails.getUsername());
        
        try {
            // Validate new password and confirm password match
            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "Mật khẩu mới và xác nhận mật khẩu không khớp");
                return "redirect:/admin/profile/change-password";
            }
            
            // Validate password length
            if (newPassword.length() < 6) {
                redirectAttributes.addFlashAttribute("error", "Mật khẩu mới phải có ít nhất 6 ký tự");
                return "redirect:/admin/profile/change-password";
            }
            
            // Change password
            userService.changePassword(user.getId(), currentPassword, newPassword);
            redirectAttributes.addFlashAttribute("success", "Đổi mật khẩu thành công");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/profile/change-password";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/admin/profile/change-password";
        }
        
        return "redirect:/admin/profile";
    }

    /**
     * Admin - Show Change Password Form
     * GET /admin/profile/change-password
     */
    @GetMapping("/admin/profile/change-password")
    public String adminChangePasswordForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("currentUser", user);
        return "admin/change-password";
    }
    
    // ========================================
    // STAFF ROUTES
    // ========================================
    
    /**
     * Staff Dashboard - Hiển thị bookings overview
     * GET /staff/dashboard
     */
    @GetMapping("/staff/dashboard")
    public String staffDashboard(Model model) {
        var bookings = bookingService.getAllBookings();
        model.addAttribute("bookings", bookings);
        addBookingStatusCounts(model, bookings);
        return "staff/dashboard";
    }

    /**
     * Tính toán số lượng booking theo trạng thái (case-insensitive)
     */
    private void addBookingStatusCounts(Model model, List<Booking> bookings) {
        int pending = 0, approved = 0, rejected = 0, cancelled = 0;
        if (bookings != null) {
            for (Booking b : bookings) {
                String status = b.getStatusString();
                if (status == null) continue;
                String normalized = status.trim().toUpperCase();
                switch (normalized) {
                    case "PENDING" -> pending++;
                    case "APPROVED" -> approved++;
                    case "REJECTED" -> rejected++;
                    case "CANCELLED" -> cancelled++;
                }
            }
        }
        model.addAttribute("pendingCount", pending);
        model.addAttribute("approvedCount", approved);
        model.addAttribute("rejectedCount", rejected);
        model.addAttribute("cancelledCount", cancelled);
    }

    /**
     * Staff - Quản lý support tickets
     * GET /staff/support
     */
    @GetMapping("/staff/support")
    public String staffManageSupport(Model model) {
        model.addAttribute("tickets", supportService.getAllTickets());
        return "staff/support";
    }
    
    /**
     * Staff Profile - Xem profile
     * GET /staff/profile
     */
    @GetMapping("/staff/profile")
    public String staffProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("currentUser", user);
        model.addAttribute("pageTitle", "Hồ sơ của tôi");
        return "staff/profile";
    }
    
    /**
     * Staff - Edit Profile form
     * GET /staff/profile/edit
     */
    @GetMapping("/staff/profile/edit")
    public String staffEditProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("currentUser", user);
        return "staff/profile-edit";
    }
    
    /**
     * Staff - Update Profile
     * POST /staff/profile/update
     */
    @PostMapping("/staff/profile/update")
    public String staffUpdateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam(required = false) String address,
            RedirectAttributes redirectAttributes) {
        
        User user = userService.findByUsername(userDetails.getUsername());
        
        try {
            // Validate email format
            if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                redirectAttributes.addFlashAttribute("error", "Email không hợp lệ");
                return "redirect:/staff/profile/edit";
            }

            // Check if email is already taken by another user
            User existingUser = userService.findByEmail(email);
            if (existingUser != null && !existingUser.getId().equals(user.getId())) {
                redirectAttributes.addFlashAttribute("error", "Email đã được sử dụng bởi tài khoản khác");
                return "redirect:/staff/profile/edit";
            }

            // Clean phone number
            String phoneDigits = phone.replaceAll("[\\s\\-]", "");
            if (!phoneDigits.matches("^[0-9]{10,11}$")) {
                redirectAttributes.addFlashAttribute("error", "Số điện thoại phải có 10-11 chữ số");
                return "redirect:/staff/profile/edit";
            }

            // Update user info including email
            user.setEmail(email);
            user.setFullName(fullName);
            user.setPhone(phoneDigits);
            user.setAddress(address);
            userService.updateUser(user);
            
            redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/staff/profile/edit";
        }

        return "redirect:/staff/profile";
    }

    /**
     * Staff - Change Password
     * POST /staff/profile/change-password
     */
    @PostMapping("/staff/profile/change-password")
    public String staffChangePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes) {
        
        User user = userService.findByUsername(userDetails.getUsername());
        
        try {
            // Validate new password and confirm password match
            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "Mật khẩu mới và xác nhận mật khẩu không khớp");
                return "redirect:/staff/profile/change-password";
            }
            
            // Validate password length
            if (newPassword.length() < 6) {
                redirectAttributes.addFlashAttribute("error", "Mật khẩu mới phải có ít nhất 6 ký tự");
                return "redirect:/staff/profile/change-password";
            }
            
            // Change password
            userService.changePassword(user.getId(), currentPassword, newPassword);
            redirectAttributes.addFlashAttribute("success", "Đổi mật khẩu thành công");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/staff/profile/change-password";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/staff/profile/change-password";
        }
        
        return "redirect:/staff/profile";
    }

    /**
     * Staff - Show Change Password Form
     * GET /staff/profile/change-password
     */
    @GetMapping("/staff/profile/change-password")
    public String staffChangePasswordForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("currentUser", user);
        return "staff/change-password";
    }
}
