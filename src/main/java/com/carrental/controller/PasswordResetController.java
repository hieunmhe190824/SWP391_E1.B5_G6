package com.carrental.controller;

import com.carrental.model.User;
import com.carrental.repository.UserRepository;
import com.carrental.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller để reset password hash trong database
 * CHỈ DÙNG CHO DEVELOPMENT - XÓA HOẶC BẢO VỆ TRONG PRODUCTION
 */
@Controller
@RequestMapping("/dev")
public class PasswordResetController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Reset password cho user
     * URL: /dev/reset-password?email=khachhang1@gmail.com&password=password123
     */
    @GetMapping("/reset-password")
    public String resetPassword(@RequestParam String email, @RequestParam String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        String hashedPassword = passwordEncoder.encode(password);
        user.setPassword(hashedPassword);
        userRepository.save(user);

        return "redirect:/auth/login?passwordReset=true";
    }
}

