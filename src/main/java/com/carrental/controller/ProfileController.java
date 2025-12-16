package com.carrental.controller;

import com.carrental.model.User;
import com.carrental.model.UserDocument;
import com.carrental.repository.UserRepository;
import com.carrental.service.UserDocumentService;
import com.carrental.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserDocumentService userDocumentService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get current authenticated user
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * View profile page
     */
    @GetMapping
    public String viewProfile(Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        List<UserDocument> documents = userDocumentService.getDocumentsByUser(currentUser);
        
        model.addAttribute("user", currentUser);
        model.addAttribute("documents", documents);
        return "customer/profile";
    }

    /**
     * Show edit profile form
     */
    @GetMapping("/edit")
    public String editProfileForm(Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        model.addAttribute("user", currentUser);
        return "customer/profile-edit";
    }

    /**
     * REST API endpoint để check email đã tồn tại chưa (cho profile edit, loại trừ email hiện tại)
     * @param email Email cần kiểm tra
     * @return JSON response với exists: true/false
     */
    @GetMapping("/check-email")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkEmail(@RequestParam String email) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("exists", false);
            return ResponseEntity.ok(response);
        }

        Map<String, Object> response = new HashMap<>();
        // Check if email exists and is not the current user's email
        boolean exists = userService.existsByEmail(email) && !currentUser.getEmail().equalsIgnoreCase(email);
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }

    /**
     * Update profile
     */
    @PostMapping("/update")
    public String updateProfile(
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam(required = false) String address,
            RedirectAttributes redirectAttributes) {
        
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        try {
            // Validate email format
            if (email == null || email.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng nhập email");
                return "redirect:/profile/edit";
            }
            
            if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                redirectAttributes.addFlashAttribute("error", "Email không hợp lệ");
                return "redirect:/profile/edit";
            }

            // Clean phone number
            String phoneDigits = phone.replaceAll("[\\s\\-]", "");
            if (!phoneDigits.matches("^[0-9]{10,11}$")) {
                redirectAttributes.addFlashAttribute("error", "Số điện thoại phải có 10-11 chữ số");
                return "redirect:/profile/edit";
            }

            userService.updateProfile(currentUser.getId(), fullName, email.trim(), phoneDigits, address);
            redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin thành công");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/profile/edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/profile/edit";
        }

        return "redirect:/profile";
    }

    /**
     * Show change password form
     */
    @GetMapping("/change-password")
    public String changePasswordForm(Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        model.addAttribute("user", currentUser);
        return "customer/change-password";
    }

    /**
     * Change password
     */
    @PostMapping("/change-password")
    public String changePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes) {
        
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        try {
            // Validate new password and confirm password match
            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "Mật khẩu mới và xác nhận mật khẩu không khớp");
                return "redirect:/profile/change-password";
            }
            
            // Validate password length
            if (newPassword.length() < 6) {
                redirectAttributes.addFlashAttribute("error", "Mật khẩu mới phải có ít nhất 6 ký tự");
                return "redirect:/profile/change-password";
            }
            
            // Change password
            userService.changePassword(currentUser.getId(), currentPassword, newPassword);
            redirectAttributes.addFlashAttribute("success", "Đổi mật khẩu thành công");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/profile/change-password";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/profile/change-password";
        }

        return "redirect:/profile";
    }

    /**
     * Show add document form
     */
    @GetMapping("/documents/add")
    public String addDocumentForm(Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        model.addAttribute("document", new UserDocument());
        return "customer/document-add";
    }

    /**
     * Add new document
     */
    @PostMapping("/documents/add")
    public String addDocument(
            @RequestParam UserDocument.DocumentType documentType,
            @RequestParam String documentNumber,
            @RequestParam(required = false) LocalDate expiryDate,
            @RequestParam(required = false) MultipartFile imageFile,
            RedirectAttributes redirectAttributes) {
        
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        try {
            userDocumentService.createDocument(currentUser, documentType, documentNumber, expiryDate, imageFile);
            redirectAttributes.addFlashAttribute("success", "Thêm giấy tờ thành công. Vui lòng chờ phê duyệt.");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi upload ảnh: " + e.getMessage());
            return "redirect:/profile/documents/add";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/profile/documents/add";
        }

        return "redirect:/profile";
    }

    /**
     * Show edit document form
     */
    @GetMapping("/documents/{id}/edit")
    public String editDocumentForm(@PathVariable Long id, Model model) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        UserDocument document = userDocumentService.getDocumentByIdAndUser(id, currentUser)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        model.addAttribute("document", document);
        return "customer/document-edit";
    }

    /**
     * Update document
     */
    @PostMapping("/documents/{id}/update")
    public String updateDocument(
            @PathVariable Long id,
            @RequestParam String documentNumber,
            @RequestParam(required = false) LocalDate expiryDate,
            @RequestParam(required = false) MultipartFile imageFile,
            RedirectAttributes redirectAttributes) {
        
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        try {
            userDocumentService.updateDocument(id, currentUser, documentNumber, expiryDate, imageFile);
            redirectAttributes.addFlashAttribute("success", "Cập nhật giấy tờ thành công");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi upload ảnh: " + e.getMessage());
            return "redirect:/profile/documents/" + id + "/edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/profile/documents/" + id + "/edit";
        }

        return "redirect:/profile";
    }

    /**
     * Delete document
     */
    @PostMapping("/documents/{id}/delete")
    public String deleteDocument(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        try {
            userDocumentService.deleteDocument(id, currentUser);
            redirectAttributes.addFlashAttribute("success", "Xóa giấy tờ thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
        }

        return "redirect:/profile";
    }
}
