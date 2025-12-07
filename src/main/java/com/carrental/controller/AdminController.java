package com.carrental.controller;

import com.carrental.model.User;
import com.carrental.service.BookingService;
import com.carrental.service.UserService;
import com.carrental.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private BookingService bookingService;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        // Load statistics cho dashboard (sẽ implement UI sau)
        try {
        model.addAttribute("totalUsers", userService.getAllUsers().size());
        model.addAttribute("totalVehicles", vehicleService.getAllVehicles().size());
        model.addAttribute("totalBookings", bookingService.getAllBookings().size());
        } catch (Exception e) {
            // Nếu có lỗi, set giá trị mặc định
            model.addAttribute("totalUsers", 0);
            model.addAttribute("totalVehicles", 0);
            model.addAttribute("totalBookings", 0);
        }
        
        // Add current user for header
        if (userDetails != null) {
            User currentUser = userService.findByUsername(userDetails.getUsername());
            model.addAttribute("currentUser", currentUser);
        }
        
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String manageUsers(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails != null) {
            User currentUser = userService.findByUsername(userDetails.getUsername());
            model.addAttribute("currentUser", currentUser);
        }
        model.addAttribute("users", userService.getAllUsers());
        return "admin/users";
    }

    @GetMapping("/vehicles")
    public String manageVehicles(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails != null) {
            User currentUser = userService.findByUsername(userDetails.getUsername());
            model.addAttribute("currentUser", currentUser);
        }
        model.addAttribute("vehicles", vehicleService.getAllVehicles());
        return "admin/vehicles";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("currentUser", user);
        model.addAttribute("pageTitle", "Hồ sơ của tôi");
        return "admin/profile";
    }

    @GetMapping("/profile/edit")
    public String editProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("currentUser", user);
        return "admin/profile-edit";
    }

    @PostMapping("/profile/update")
    public String updateProfile(
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
}
