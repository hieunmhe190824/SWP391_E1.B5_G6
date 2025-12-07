package com.carrental.controller;

import com.carrental.model.User;
import com.carrental.service.BookingService;
import com.carrental.service.SupportService;
import com.carrental.service.UserService;
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
@RequestMapping("/staff")
public class StaffController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private SupportService supportService;

    @Autowired
    private UserService userService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("bookings", bookingService.getAllBookings());
        return "staff/dashboard";
    }

    @GetMapping("/bookings")
    public String manageBookings(Model model) {
        model.addAttribute("bookings", bookingService.getAllBookings());
        return "staff/bookings";
    }

    @GetMapping("/support")
    public String manageSupport(Model model) {
        model.addAttribute("tickets", supportService.getAllTickets());
        return "staff/support";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("currentUser", user);
        model.addAttribute("pageTitle", "Hồ sơ của tôi");
        return "staff/profile";
    }

    @GetMapping("/profile/edit")
    public String editProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername());
        model.addAttribute("user", user);
        model.addAttribute("currentUser", user);
        return "staff/profile-edit";
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
}
