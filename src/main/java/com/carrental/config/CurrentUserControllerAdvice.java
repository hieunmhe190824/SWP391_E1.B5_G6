package com.carrental.config;

import com.carrental.model.User;
import com.carrental.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class CurrentUserControllerAdvice {

    @Autowired
    private UserRepository userRepository;

    @ModelAttribute("currentUser")
    public User getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return null;
            }
            
            String name = authentication.getName();
            if (name == null || name.equals("anonymousUser")) {
                return null;
            }
            
            return userRepository.findByEmail(name).orElse(null);
        } catch (Exception e) {
            // Log error but don't throw - return null to prevent breaking the page
            System.err.println("Error getting current user: " + e.getMessage());
            return null;
        }
    }
}
