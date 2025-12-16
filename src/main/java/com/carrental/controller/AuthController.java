package com.carrental.controller;

import com.carrental.model.User;
import com.carrental.service.EmailService;
import com.carrental.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("user", new User());
        return "auth/register";
    }

    /**
     * REST API endpoint để check email đã tồn tại chưa (real-time validation)
     * @param email Email cần kiểm tra
     * @return JSON response với exists: true/false
     */
    @GetMapping("/check-email")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkEmail(@RequestParam String email) {
        Map<String, Object> response = new HashMap<>();
        boolean exists = userService.existsByEmail(email);
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public String register(
            User user,
            @RequestParam("confirmPassword") String confirmPassword,
            Model model) {
        
        // Validation: Chỉ kiểm tra các trường NOT NULL trong database
        // Database: email, password_hash, full_name, phone là NOT NULL
        // address có thể NULL nên không cần check
        
        // full_name: NOT NULL trong DB
        if (user.getFullName() == null || user.getFullName().trim().isEmpty()) {
            model.addAttribute("error", "Vui lòng nhập họ và tên");
            model.addAttribute("user", user);
            return "auth/register";
        }

        // email: NOT NULL trong DB
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            model.addAttribute("error", "Vui lòng nhập email");
            model.addAttribute("user", user);
            return "auth/register";
        }

        // Validation: Kiểm tra định dạng email
        if (!user.getEmail().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            model.addAttribute("error", "Email không hợp lệ");
            model.addAttribute("user", user);
            return "auth/register";
        }

        // Validation: Kiểm tra email đã tồn tại
        if (userService.existsByEmail(user.getEmail())) {
            model.addAttribute("error", "Email này đã được sử dụng");
            model.addAttribute("user", user);
            return "auth/register";
        }

        // phone: NOT NULL trong DB
        if (user.getPhone() == null || user.getPhone().trim().isEmpty()) {
            model.addAttribute("error", "Vui lòng nhập số điện thoại");
            model.addAttribute("user", user);
            return "auth/register";
        }

        // Validation: Kiểm tra định dạng số điện thoại (10-11 chữ số, chỉ số, không chữ cái và ký tự đặc biệt)
        // Loại bỏ khoảng trắng và dấu gạch ngang để kiểm tra
        String phoneDigits = user.getPhone().replaceAll("[\\s\\-]", "");
        // Kiểm tra chỉ chứa số và độ dài 10-11
        if (!phoneDigits.matches("^[0-9]{10,11}$")) {
            model.addAttribute("error", "Số điện thoại phải có 10-11 chữ số, không được chứa chữ cái và ký tự đặc biệt");
            model.addAttribute("user", user);
            return "auth/register";
        }
        // Kiểm tra input gốc không chứa chữ cái (a-z, A-Z)
        if (user.getPhone().matches(".*[a-zA-Z].*")) {
            model.addAttribute("error", "Số điện thoại không được chứa chữ cái");
            model.addAttribute("user", user);
            return "auth/register";
        }
        // Lưu lại số điện thoại đã được làm sạch (chỉ số)
        user.setPhone(phoneDigits);

        // password: NOT NULL trong DB (password_hash)
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            model.addAttribute("error", "Vui lòng nhập mật khẩu");
            model.addAttribute("user", user);
            return "auth/register";
        }

        // Validation: Kiểm tra độ mạnh mật khẩu (ít nhất 8 ký tự, có chữ hoa, chữ thường và số)
        // Cho phép ký tự đặc biệt
        if (!user.getPassword().matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$")) {
            model.addAttribute("error", "Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường và số");
            model.addAttribute("user", user);
            return "auth/register";
        }

        // Validation: Kiểm tra xác nhận mật khẩu (không có trong DB nhưng cần validate)
        if (confirmPassword == null || confirmPassword.trim().isEmpty()) {
            model.addAttribute("error", "Vui lòng xác nhận mật khẩu");
            model.addAttribute("user", user);
            return "auth/register";
        }

        if (!user.getPassword().equals(confirmPassword)) {
            model.addAttribute("error", "Mật khẩu xác nhận không khớp");
            model.addAttribute("user", user);
            return "auth/register";
        }

        // address: Có thể NULL trong DB, không cần check null

        // Tạo tài khoản
        user.setRole(User.UserRole.CUSTOMER);
        user.setStatus(User.UserStatus.ACTIVE);
        userService.createUser(user);

        return "redirect:/auth/login?registered";
    }
    
    /**
     * Show forgot password form
     */
    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "auth/forgot-password";
    }
    
    /**
     * Process forgot password request
     * @param email User email
     * @param model Model for view
     * @return Redirect to login page with success message or back to form with error
     */
    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam String email, Model model) {
        try {
            // Validate email format
            if (email == null || email.trim().isEmpty()) {
                model.addAttribute("error", "Vui lòng nhập email");
                return "auth/forgot-password";
            }
            
            if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                model.addAttribute("error", "Email không hợp lệ");
                return "auth/forgot-password";
            }
            
            // Reset password and get new password
            String newPassword = userService.resetPasswordByEmail(email);
            
            // Send email with new password using EmailService (SMTP)
            emailService.sendPasswordResetEmail(email, newPassword);
            
            // Redirect to login with success message
            return "redirect:/auth/login?passwordReset=true";
            
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/forgot-password";
        } catch (Exception e) {
            model.addAttribute("error", "Đã xảy ra lỗi. Vui lòng thử lại sau.");
            System.err.println("Error in forgot password: " + e.getMessage());
            return "auth/forgot-password";
        }
    }
}
