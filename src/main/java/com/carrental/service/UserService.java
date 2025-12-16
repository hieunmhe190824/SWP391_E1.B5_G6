package com.carrental.service;

import com.carrental.model.User;
import com.carrental.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    // Track last password reset time per email to prevent duplicate resets
    private final ConcurrentHashMap<String, Long> lastResetTime = new ConcurrentHashMap<>();
    private static final long RESET_COOLDOWN_MS = 60000; // 60 seconds

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User createUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public User updateUser(Long id, User userDetails) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setFullName(userDetails.getFullName());
        user.setEmail(userDetails.getEmail());
        user.setPhone(userDetails.getPhone());
        user.setAddress(userDetails.getAddress());

        return userRepository.save(user);
    }

    public User updateUser(User user) {
        return userRepository.save(user);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public User updateProfile(Long id, String fullName, String email, String phone, String address) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if email is being changed and if new email already exists
        if (!user.getEmail().equalsIgnoreCase(email) && existsByEmail(email)) {
            throw new IllegalArgumentException("Email đã được sử dụng bởi tài khoản khác");
        }

        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setAddress(address);

        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public User findByUsername(String username) {
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
    }
    
    /**
     * Change user password
     * @param userId User ID
     * @param currentPassword Current password for verification
     * @param newPassword New password to set
     * @throws IllegalArgumentException if current password is incorrect
     */
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng");
        }
        
        // Encode and set new password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
    
    /**
     * Find all staff members (for ticket assignment)
     */
    public List<User> findAllStaff() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRole() == User.UserRole.STAFF || user.getRole() == User.UserRole.ADMIN)
                .toList();
    }
    
    /**
     * Generate random password (8 characters: uppercase, lowercase, digits)
     * @return Random password string
     */
    public String generateRandomPassword() {
        String upperCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCase = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String allChars = upperCase + lowerCase + digits;
        
        StringBuilder password = new StringBuilder();
        java.util.Random random = new java.util.Random();
        
        // Ensure at least one of each type
        password.append(upperCase.charAt(random.nextInt(upperCase.length())));
        password.append(lowerCase.charAt(random.nextInt(lowerCase.length())));
        password.append(digits.charAt(random.nextInt(digits.length())));
        
        // Fill remaining 5 characters randomly
        for (int i = 0; i < 5; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }
        
        // Shuffle the password
        char[] passwordArray = password.toString().toCharArray();
        for (int i = passwordArray.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[j];
            passwordArray[j] = temp;
        }
        
        return new String(passwordArray);
    }
    
    /**
     * Reset password by email
     * @param email User email
     * @return New password (plain text) to send via email
     * @throws IllegalArgumentException if email not found or reset too soon
     */
    public String resetPasswordByEmail(String email) {
        // Check if password was reset recently (within cooldown period)
        Long lastReset = lastResetTime.get(email.toLowerCase());
        long currentTime = System.currentTimeMillis();
        
        if (lastReset != null && (currentTime - lastReset) < RESET_COOLDOWN_MS) {
            throw new IllegalArgumentException("Vui lòng đợi một chút trước khi yêu cầu reset mật khẩu lại");
        }
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email không tồn tại trong hệ thống"));
        
        // Generate new random password
        String newPassword = generateRandomPassword();
        
        // Encode and save new password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        // Update last reset time
        lastResetTime.put(email.toLowerCase(), currentTime);
        
        return newPassword;
    }
}
