package com.carrental.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility class để generate password hash cho database
 * Chạy main method này để lấy hash của password "password123"
 */
public class PasswordHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "password123";
        String hashedPassword = encoder.encode(password);
        System.out.println("Password: " + password);
        System.out.println("Hashed: " + hashedPassword);
        
        // Test verify
        boolean matches = encoder.matches(password, hashedPassword);
        System.out.println("Verify: " + matches);
    }
}

