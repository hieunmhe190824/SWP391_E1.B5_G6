package com.carrental.controller;

import com.carrental.model.Contract;
import com.carrental.model.Handover;
import com.carrental.model.User;
import com.carrental.repository.UserRepository;
import com.carrental.service.ContractService;
import com.carrental.service.HandoverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class HandoverController {

    @Autowired
    private HandoverService handoverService;
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContractService contractService;

    /**
     * Check if current authenticated user has ADMIN role
     */
    private boolean isAdmin() {
        org.springframework.security.core.Authentication authentication = 
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .anyMatch(role -> role.equals("ROLE_ADMIN") || role.equals("ADMIN"));
    }

    /**
     * Get current authenticated user
     */
    private User getCurrentUser() {
        org.springframework.security.core.Authentication authentication = 
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        String email = authentication.getName();
        return getUserByEmail(email);
    }

    /**
     * UC12: Display list of contracts ready for pickup
     * Staff view
     */
    @GetMapping("/staff/handover/pickup")
    public String showPickupList(Model model) {
        List<Contract> contractsReadyForPickup;
        if (isAdmin()) {
            contractsReadyForPickup = handoverService.getContractsReadyForPickup();
        } else {
            User currentUser = getCurrentUser();
            contractsReadyForPickup = handoverService.getContractsReadyForPickupByStaff(currentUser.getId());
        }
        model.addAttribute("contracts", contractsReadyForPickup);
        return "staff/pickup-list";
    }

    /**
     * UC12: Display pickup form for a specific contract
     */
    @GetMapping("/staff/handover/pickup/{contractId}")
    public String showPickupForm(@PathVariable Long contractId, Model model, RedirectAttributes redirectAttributes) {
        try {
            Contract contract = contractService.getContractById(contractId)
                    .orElseThrow(() -> new RuntimeException("Contract not found"));

            // Check if pickup already done
            if (handoverService.hasPickupCompleted(contractId)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Pickup already completed for this contract");
                return "redirect:/staff/handover/pickup";
            }

            model.addAttribute("contract", contract);
            return "staff/handover-pickup";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/staff/handover/pickup";
        }
    }

    /**
     * UC12: Process vehicle pickup
     */
    @PostMapping("/staff/handover/pickup/{contractId}")
    public String processPickup(
            @PathVariable Long contractId,
            @RequestParam("odometer") Integer odometer,
            @RequestParam("fuelLevel") Integer fuelLevel,
            @RequestParam("conditionNotes") String conditionNotes,
            @RequestParam("images") MultipartFile[] images,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            // Get current staff user
            User staff = getUserByEmail(userDetails.getUsername());

            // Perform pickup
            handoverService.performPickup(contractId, odometer, fuelLevel, conditionNotes, images, staff);

            redirectAttributes.addFlashAttribute("successMessage", "Bàn giao xe thành công!");
            return "redirect:/staff/rentals/active";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Validation error: " + e.getMessage());
            return "redirect:/staff/handover/pickup/" + contractId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error during pickup: " + e.getMessage());
            return "redirect:/staff/handover/pickup/" + contractId;
        }
    }

    /**
     * UC12: Cancel contract and refund (when pickup fails)
     */
    @PostMapping("/staff/handover/pickup/{contractId}/cancel")
    public String cancelPickupAndRefund(
            @PathVariable Long contractId,
            @RequestParam("reason") String reason,
            RedirectAttributes redirectAttributes) {
        try {
            handoverService.cancelContractAndRefund(contractId, reason);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Contract cancelled and refund initiated. Reason: " + reason);
            return "redirect:/staff/contracts";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error cancelling contract: " + e.getMessage());
            return "redirect:/staff/handover/pickup/" + contractId;
        }
    }

    /**
     * UC13: Track active rentals - Staff view
     */
    @GetMapping("/staff/rentals/active")
    public String showActiveRentalsStaff(Model model) {
        List<Contract> activeRentals;
        if (isAdmin()) {
            activeRentals = handoverService.getActiveRentals();
        } else {
            User currentUser = getCurrentUser();
            activeRentals = handoverService.getActiveRentalsByStaff(currentUser.getId());
        }
        model.addAttribute("contracts", activeRentals);
        model.addAttribute("pageTitle", "Active Rentals");
        return "staff/active-rentals";
    }

    /**
     * UC13: Track active rentals - Customer view
     */
    @GetMapping("/customer/rentals/active")
    public String showActiveRentalsCustomer(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        User customer = getUserByEmail(userDetails.getUsername());
        List<Contract> activeRentals = handoverService.getActiveRentalsByCustomer(customer.getId());
        model.addAttribute("contracts", activeRentals);
        return "customer/active-rentals";
    }

    /**
     * UC13: Track active rentals - Admin view
     */
    @GetMapping("/admin/rentals/active")
    public String showActiveRentalsAdmin(Model model) {
        List<Contract> activeRentals = handoverService.getActiveRentals();
        model.addAttribute("contracts", activeRentals);
        model.addAttribute("pageTitle", "All Active Rentals");
        return "admin/active-rentals";
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    // Legacy methods (keep for backward compatibility)
    @GetMapping("/handovers/create")
    public String createHandoverPage(@RequestParam Long contractId, Model model) {
        model.addAttribute("contractId", contractId);
        model.addAttribute("handover", new Handover());
        return "staff/handover";
    }

    @PostMapping("/handovers/create")
    public String createHandover(@ModelAttribute Handover handover) {
        handoverService.createHandover(handover);
        return "redirect:/staff/dashboard";
    }
}
