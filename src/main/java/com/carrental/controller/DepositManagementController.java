package com.carrental.controller;

import com.carrental.model.*;
import com.carrental.model.Refund.RefundMethod;
import com.carrental.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping({"/staff/deposits", "/admin/deposits"})
public class DepositManagementController {

    @Autowired
    private DepositService depositService;

    @Autowired
    private TrafficViolationService violationService;

    @Autowired
    private RefundService refundService;

    @Autowired
    private com.carrental.repository.UserRepository userRepository;

    /**
     * Get current authenticated user
     */
    private com.carrental.model.User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * Check if current authenticated user has ADMIN role
     */
    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN") || role.equals("ADMIN"));
    }

    /**
     * UC16, UC17, UC18: List all deposit holds
     */
    @GetMapping("/holds")
    public String listDepositHolds(
            @RequestParam(value = "status", required = false) String statusStr,
            Model model) {
        
        System.out.println("=== DEPOSIT FILTER DEBUG ===");
        System.out.println("Status parameter: '" + statusStr + "'");
        
        List<DepositHold> deposits;
        com.carrental.model.User currentUser = getCurrentUser();
        
        if (isAdmin()) {
            // Admin sees all deposits
            if (statusStr != null && !statusStr.isEmpty()) {
                try {
                    // Convert to uppercase to handle case-insensitive input
                    DepositHold.DepositStatus status = DepositHold.DepositStatus.valueOf(statusStr.toUpperCase());
                    System.out.println("Parsed status enum: " + status);
                    deposits = depositService.getDepositsByStatus(status);
                    System.out.println("Found " + deposits.size() + " deposits with status: " + status);
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid status value: " + statusStr);
                    deposits = depositService.getAllDeposits();
                    statusStr = null; // Reset to show all
                }
            } else {
                System.out.println("No status filter, showing all deposits");
                deposits = depositService.getAllDeposits();
            }
        } else {
            // Staff only sees deposits for contracts assigned to them
            if (statusStr != null && !statusStr.isEmpty()) {
                try {
                    DepositHold.DepositStatus status = DepositHold.DepositStatus.valueOf(statusStr.toUpperCase());
                    System.out.println("Parsed status enum: " + status);
                    deposits = depositService.getDepositsByStaffIdAndStatus(currentUser.getId(), status);
                    System.out.println("Found " + deposits.size() + " deposits with status: " + status + " for staff: " + currentUser.getId());
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid status value: " + statusStr);
                    deposits = depositService.getDepositsByStaffId(currentUser.getId());
                    statusStr = null; // Reset to show all
                }
            } else {
                System.out.println("No status filter, showing all deposits for staff: " + currentUser.getId());
                deposits = depositService.getDepositsByStaffId(currentUser.getId());
            }
        }
        
        // Auto-update deposits from HOLDING to READY when holdEndDate has passed
        // This ensures deposits are only eligible for refund 2 weeks after actual return date
        LocalDateTime now = LocalDateTime.now();
        for (DepositHold deposit : deposits) {
            if (deposit.getStatus() == DepositHold.DepositStatus.HOLDING 
                && deposit.getHoldEndDate() != null 
                && now.isAfter(deposit.getHoldEndDate())) {
                
                System.out.println("Auto-updating deposit " + deposit.getId() + " from HOLDING to READY");
                System.out.println("  Hold end date: " + deposit.getHoldEndDate());
                System.out.println("  Current time: " + now);
                
                depositService.updateDepositStatus(deposit.getId(), DepositHold.DepositStatus.READY);
                deposit.setStatus(DepositHold.DepositStatus.READY); // Update in-memory object too
            }
        }
        
        // Check which deposits already have refunds processed
        // AND auto-update status to REFUNDED if refund exists
        java.util.Map<Long, Boolean> refundProcessedMap = new java.util.HashMap<>();
        for (DepositHold deposit : deposits) {
            boolean hasRefund = refundService.getRefundByDepositHold(deposit.getId()).isPresent();
            refundProcessedMap.put(deposit.getId(), hasRefund);
            
            if (hasRefund) {
                System.out.println("Deposit " + deposit.getId() + " already has refund processed");
                
                // Auto-update status to REFUNDED if not already
                if (deposit.getStatus() != DepositHold.DepositStatus.REFUNDED) {
                    System.out.println("  Auto-updating deposit " + deposit.getId() + " to REFUNDED status");
                    depositService.updateDepositStatus(deposit.getId(), DepositHold.DepositStatus.REFUNDED);
                    deposit.setStatus(DepositHold.DepositStatus.REFUNDED); // Update in-memory object too
                }
            }
        }
        
        // Filter out deposits that already have refunds when status is READY
        // This ensures the READY filter only shows deposits that can actually be refunded
        if (statusStr != null && statusStr.equalsIgnoreCase("READY")) {
            deposits = deposits.stream()
                .filter(deposit -> !refundProcessedMap.getOrDefault(deposit.getId(), false))
                .collect(java.util.stream.Collectors.toList());
            System.out.println("After filtering out refunded deposits: " + deposits.size() + " deposits remaining");
        }
        
        System.out.println("Total deposits to display: " + deposits.size());
        System.out.println("=== END FILTER DEBUG ===");

        model.addAttribute("deposits", deposits);
        model.addAttribute("selectedStatus", statusStr);
        model.addAttribute("refundProcessed", refundProcessedMap);

        if (isAdmin()) {
            return "admin/deposit-holds";
        }
        return "staff/deposit-holds";
    }

    /**
     * UC17: View deposit hold details and manage violations
     */
    @GetMapping("/{holdId}")
    public String viewDepositDetail(@PathVariable Long holdId, Model model, RedirectAttributes redirectAttributes) {
        try {
            DepositHold depositHold = depositService.getDepositById(holdId)
                    .orElseThrow(() -> new RuntimeException("Deposit hold not found"));

            // Get violations
            List<TrafficViolation> violations = violationService.getViolationsByDepositHold(holdId);
            
            // Get total fines
            BigDecimal totalFines = violationService.getTotalFinesByHold(holdId);

            // Calculate refund preview
            BigDecimal refundPreview = refundService.calculateRefundAmount(holdId);

            model.addAttribute("depositHold", depositHold);
            model.addAttribute("violations", violations);
            model.addAttribute("totalFines", totalFines);
            model.addAttribute("refundPreview", refundPreview);

            if (isAdmin()) {
                return "admin/deposit-detail";
            }
            return "staff/deposit-detail";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            if (isAdmin()) {
                return "redirect:/admin/deposits/holds";
            }
            return "redirect:/staff/deposits/holds";
        }
    }

    /**
     * UC17: Add traffic violation
     */
    @PostMapping("/{holdId}/violations")
    public String addViolation(
            @PathVariable Long holdId,
            @RequestParam("violationType") String violationType,
            @RequestParam("violationDate") String violationDateStr,
            @RequestParam("fineAmount") BigDecimal fineAmount,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "evidenceFile", required = false) MultipartFile evidenceFile,
            RedirectAttributes redirectAttributes) {
        try {
            // Parse violation date
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
            LocalDateTime violationDate = LocalDateTime.parse(violationDateStr, formatter);

            // Create violation
            violationService.createViolation(holdId, violationType, violationDate, 
                    fineAmount, description, evidenceFile);

            redirectAttributes.addFlashAttribute("successMessage", "Thêm vi phạm thành công!");
            if (isAdmin()) {
                return "redirect:/admin/deposits/" + holdId;
            }
            return "redirect:/staff/deposits/" + holdId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi thêm vi phạm: " + e.getMessage());
            if (isAdmin()) {
                return "redirect:/admin/deposits/" + holdId;
            }
            return "redirect:/staff/deposits/" + holdId;
        }
    }

    /**
     * UC17: Delete traffic violation
     */
    @PostMapping("/violations/{violationId}/delete")
    public String deleteViolation(
            @PathVariable Long violationId,
            @RequestParam("holdId") Long holdId,
            RedirectAttributes redirectAttributes) {
        try {
            violationService.deleteViolation(violationId);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa vi phạm thành công!");
            if (isAdmin()) {
                return "redirect:/admin/deposits/" + holdId;
            }
            return "redirect:/staff/deposits/" + holdId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa vi phạm: " + e.getMessage());
            if (isAdmin()) {
                return "redirect:/admin/deposits/" + holdId;
            }
            return "redirect:/staff/deposits/" + holdId;
        }
    }

    /**
     * UC18: Show refund processing page
     */
    @GetMapping("/{holdId}/refund")
    public String showRefundPage(@PathVariable Long holdId, Model model, RedirectAttributes redirectAttributes) {
        try {
            DepositHold depositHold = depositService.getDepositById(holdId)
                    .orElseThrow(() -> new RuntimeException("Deposit hold not found"));

            // Verify status is READY
            if (depositHold.getStatus() != DepositHold.DepositStatus.READY) {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Deposit hold is not ready for refund. Current status: " + depositHold.getStatus());
                if (isAdmin()) {
                    return "redirect:/admin/deposits/" + holdId;
                }
                return "redirect:/staff/deposits/" + holdId;
            }

            // Check if refund already processed
            if (refundService.getRefundByDepositHold(holdId).isPresent()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Refund already processed");
                if (isAdmin()) {
                    return "redirect:/admin/deposits/" + holdId;
                }
                return "redirect:/staff/deposits/" + holdId;
            }

            // Get violations
            List<TrafficViolation> violations = violationService.getViolationsByDepositHold(holdId);
            BigDecimal totalFines = violationService.getTotalFinesByHold(holdId);

            // Calculate refund amount
            BigDecimal refundAmount = refundService.calculateRefundAmount(holdId);

            model.addAttribute("depositHold", depositHold);
            model.addAttribute("violations", violations);
            model.addAttribute("totalFines", totalFines);
            model.addAttribute("refundAmount", refundAmount);

            if (isAdmin()) {
                return "admin/refund-process";
            }
            return "staff/refund-process";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            if (isAdmin()) {
                return "redirect:/admin/deposits/holds";
            }
            return "redirect:/staff/deposits/holds";
        }
    }


    /**
     * UC18: Process deposit refund
     */
    @PostMapping("/{holdId}/refund")
    public String processRefund(
            @PathVariable Long holdId,
            @RequestParam("refundMethod") String refundMethodStr,
            RedirectAttributes redirectAttributes) {
        try {
            // Log the received parameter
            System.out.println("=== REFUND PROCESSING DEBUG ===");
            System.out.println("Hold ID: " + holdId);
            System.out.println("Refund Method String (raw): '" + refundMethodStr + "'");
            
            // Parse refund method (convert to uppercase to handle case-insensitive input)
            RefundMethod refundMethod = RefundMethod.valueOf(refundMethodStr.toUpperCase());
            System.out.println("Parsed RefundMethod enum: " + refundMethod);

            // Process refund (violations should already be added via /staff/violations page)
            Refund refund = refundService.processRefund(holdId, refundMethod);
            System.out.println("Refund processed successfully. Refund ID: " + refund.getId());
            System.out.println("=== END DEBUG ===");

            redirectAttributes.addFlashAttribute("successMessage", 
                String.format("Hoàn tiền thành công! Số tiền: %,.0f VNĐ", refund.getRefundAmount()));
            if (isAdmin()) {
                return "redirect:/admin/deposits/" + holdId;
            }
            return "redirect:/staff/deposits/" + holdId;
        } catch (Exception e) {
            System.err.println("=== REFUND ERROR ===");
            System.err.println("Error type: " + e.getClass().getName());
            System.err.println("Error message: " + e.getMessage());
            e.printStackTrace();
            System.err.println("=== END ERROR ===");
            
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi hoàn tiền: " + e.getMessage());
            if (isAdmin()) {
                return "redirect:/admin/deposits/" + holdId + "/refund";
            }
            return "redirect:/staff/deposits/" + holdId + "/refund";
        }
    }
}
