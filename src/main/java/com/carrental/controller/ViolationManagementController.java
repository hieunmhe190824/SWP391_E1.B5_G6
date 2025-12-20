package com.carrental.controller;

import com.carrental.model.*;
import com.carrental.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Controller
@RequestMapping({"/staff/violations", "/admin/violations"})
public class ViolationManagementController {

    @Autowired
    private DepositService depositService;

    @Autowired
    private TrafficViolationService violationService;

    @Autowired
    private com.carrental.repository.UserRepository userRepository;

    @Autowired
    private RefundService refundService;

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
     * UC17: Show violations management page
     * Displays all bookings with filter options (status filter)
     */
    @GetMapping
    public String showViolationsPage(
            @RequestParam(value = "status", required = false) String statusStr,
            Model model) {
        try {
            com.carrental.model.User currentUser = getCurrentUser();
            List<DepositHold> deposits;
            
            if (isAdmin()) {
                // Admin sees all deposits
                if (statusStr != null && !statusStr.isEmpty()) {
                    try {
                        DepositHold.DepositStatus status = DepositHold.DepositStatus.valueOf(statusStr.toUpperCase());
                        deposits = depositService.getDepositsByStatus(status);
                    } catch (IllegalArgumentException e) {
                        deposits = depositService.getAllDeposits();
                        statusStr = null; // Reset to show all
                    }
                } else {
                    deposits = depositService.getAllDeposits();
                }
            } else {
                // Staff only sees deposits for contracts assigned to them
                if (statusStr != null && !statusStr.isEmpty()) {
                    try {
                        DepositHold.DepositStatus status = DepositHold.DepositStatus.valueOf(statusStr.toUpperCase());
                        deposits = depositService.getDepositsByStaffIdAndStatus(currentUser.getId(), status);
                    } catch (IllegalArgumentException e) {
                        deposits = depositService.getDepositsByStaffId(currentUser.getId());
                        statusStr = null; // Reset to show all
                    }
                } else {
                    deposits = depositService.getDepositsByStaffId(currentUser.getId());
                }
            }
            
            // Auto-update deposits from HOLDING to READY when holdEndDate has passed
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            for (DepositHold deposit : deposits) {
                if (deposit.getStatus() == DepositHold.DepositStatus.HOLDING 
                    && deposit.getHoldEndDate() != null 
                    && now.isAfter(deposit.getHoldEndDate())) {
                    depositService.updateDepositStatus(deposit.getId(), DepositHold.DepositStatus.READY);
                    deposit.setStatus(DepositHold.DepositStatus.READY);
                }
            }
            
            // Check which deposits already have refunds processed
            Map<Long, Boolean> refundProcessedMap = new HashMap<>();
            for (DepositHold deposit : deposits) {
                boolean hasRefund = refundService.getRefundByDepositHold(deposit.getId()).isPresent();
                refundProcessedMap.put(deposit.getId(), hasRefund);
                
                // Auto-update status to REFUNDED if refund exists
                if (hasRefund && deposit.getStatus() != DepositHold.DepositStatus.REFUNDED) {
                    depositService.updateDepositStatus(deposit.getId(), DepositHold.DepositStatus.REFUNDED);
                    deposit.setStatus(DepositHold.DepositStatus.REFUNDED);
                }
            }
            
            // Filter out deposits that already have refunds when status is READY
            if (statusStr != null && statusStr.equalsIgnoreCase("READY")) {
                deposits = deposits.stream()
                    .filter(deposit -> !refundProcessedMap.getOrDefault(deposit.getId(), false))
                    .collect(java.util.stream.Collectors.toList());
            }
            
            // Get violations for each deposit
            Map<Long, List<TrafficViolation>> violationsMap = new HashMap<>();
            Map<Long, BigDecimal> totalFinesMap = new HashMap<>();
            Map<Long, BigDecimal> refundAmountMap = new HashMap<>();
            
            for (DepositHold deposit : deposits) {
                List<TrafficViolation> violations = violationService.getViolationsByDepositHold(deposit.getId());
                violationsMap.put(deposit.getId(), violations);
                totalFinesMap.put(deposit.getId(), violationService.getTotalFinesByHold(deposit.getId()));
                // Calculate refund amount for each deposit (only if status is READY)
                if (deposit.getStatus() == DepositHold.DepositStatus.READY) {
                    refundAmountMap.put(deposit.getId(), refundService.calculateRefundAmount(deposit.getId()));
                } else {
                    refundAmountMap.put(deposit.getId(), BigDecimal.ZERO);
                }
            }
            
            model.addAttribute("deposits", deposits);
            model.addAttribute("violationsMap", violationsMap);
            model.addAttribute("totalFinesMap", totalFinesMap);
            model.addAttribute("refundAmountMap", refundAmountMap);
            model.addAttribute("selectedStatus", statusStr);
            model.addAttribute("refundProcessed", refundProcessedMap);
            
            if (isAdmin()) {
                return "admin/violations";
            }
            return "staff/violations";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Lỗi khi tải trang: " + e.getMessage());
            if (isAdmin()) {
                return "admin/violations";
            }
            return "staff/violations";
        }
    }
}

