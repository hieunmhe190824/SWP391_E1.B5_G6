package com.carrental.controller;

import com.carrental.model.*;
import com.carrental.repository.LocationRepository;
import com.carrental.repository.UserRepository;
import com.carrental.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping({"/staff/returns", "/admin/returns"})
public class ReturnController {

    @Autowired
    private ReturnService returnService;

    @Autowired
    private ContractService contractService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private UserRepository userRepository;

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
     * UC14: Show list of contracts ready for return
     */
    @GetMapping("/list")
    public String showReturnList(Model model) {
        List<Contract> contractsReadyForReturn;
        if (isAdmin()) {
            contractsReadyForReturn = returnService.getContractsReadyForReturn();
        } else {
            com.carrental.model.User currentUser = getCurrentUser();
            contractsReadyForReturn = returnService.getContractsReadyForReturnByStaff(currentUser.getId());
        }
        model.addAttribute("contracts", contractsReadyForReturn);
        if (isAdmin()) {
            return "admin/return-list";
        }
        return "staff/return-list";
    }

    /**
     * UC14: Show return form for a specific contract
     */
    @GetMapping("/{contractId}")
    public String showReturnForm(@PathVariable Long contractId, Model model, RedirectAttributes redirectAttributes) {
        try {
            Contract contract = contractService.getContractById(contractId)
                    .orElseThrow(() -> new RuntimeException("Contract not found"));

            // Check if return already done
            if (returnService.hasReturnCompleted(contractId)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Return already completed for this contract");
                return "redirect:/staff/returns/list";
            }

            // Ensure booking and returnLocation are loaded (lazy loading)
            if (contract.getBooking() != null && contract.getBooking().getReturnLocation() != null) {
                // Access to trigger lazy loading
                contract.getBooking().getReturnLocation().getId();
            }

            // Get all locations for dropdown
            List<Location> locations = locationRepository.findAll();

            model.addAttribute("contract", contract);
            model.addAttribute("locations", locations);
            if (isAdmin()) {
                return "admin/return-form";
            }
            return "staff/return-form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            if (isAdmin()) {
                return "redirect:/admin/returns/list";
            }
            return "redirect:/staff/returns/list";
        }
    }

    /**
     * UC14: Process vehicle return
     */
    @PostMapping("/{contractId}")
    public String processReturn(
            @PathVariable Long contractId,
            @RequestParam("actualReturnDate") String actualReturnDateStr,
            @RequestParam("odometer") Integer odometer,
            @RequestParam("fuelLevel") Integer fuelLevel,
            @RequestParam("conditionNotes") String conditionNotes,
            @RequestParam(value = "hasDamage", required = false, defaultValue = "false") Boolean hasDamage,
            @RequestParam(value = "damageDescription", required = false) String damageDescription,
            @RequestParam(value = "damageFee", required = false) BigDecimal damageFee,
            @RequestParam("returnLocationId") Long returnLocationId,
            @RequestParam("images") MultipartFile[] images,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        try {
            // Get current staff user
            User staff = getUserByEmail(userDetails.getUsername());

            // Parse actual return date (format: yyyy-MM-ddTHH:mm)
            LocalDateTime actualReturnDate = LocalDateTime.parse(actualReturnDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));

            // Perform return (this will create bill and notification automatically)
            returnService.performReturn(contractId, actualReturnDate, odometer, fuelLevel, conditionNotes,
                    hasDamage, damageDescription, damageFee, returnLocationId, images, staff);

            redirectAttributes.addFlashAttribute("successMessage", "Trả xe thành công! Hóa đơn đã được tạo và thông báo đã gửi tới khách hàng.");
            if (isAdmin()) {
                return "redirect:/admin/returns/list";
            }
            return "redirect:/staff/returns/list";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xác thực: " + e.getMessage());
            if (isAdmin()) {
                return "redirect:/admin/returns/" + contractId;
            }
            return "redirect:/staff/returns/" + contractId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi trả xe: " + e.getMessage());
            if (isAdmin()) {
                return "redirect:/admin/returns/" + contractId;
            }
            return "redirect:/staff/returns/" + contractId;
        }
    }

    /**
     * UC15: Show rental payment page
     */
    @GetMapping("/{contractId}/payment")
    public String showRentalPayment(@PathVariable Long contractId, Model model, RedirectAttributes redirectAttributes) {
        try {
            Contract contract = contractService.getContractById(contractId)
                    .orElseThrow(() -> new RuntimeException("Contract not found"));

            // Verify contract is awaiting bill payment
            if (contract.getStatus() != Contract.ContractStatus.BILL_PENDING
                    && contract.getStatus() != Contract.ContractStatus.COMPLETED) {
                redirectAttributes.addFlashAttribute("errorMessage", "Contract is not ready for bill payment");
                if (isAdmin()) {
                    return "redirect:/admin/returns/list";
                }
                return "redirect:/staff/returns/list";
            }

            // Check if payment already done
            if (paymentService.getRentalPayment(contractId).isPresent()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Rental payment already processed");
                if (isAdmin()) {
                    return "redirect:/admin/contracts/" + contractId;
                }
                return "redirect:/staff/contracts/" + contractId;
            }

            // Get return fees
            ReturnFee returnFee = returnService.getReturnFeeByContract(contractId)
                    .orElse(null);

            model.addAttribute("contract", contract);
            model.addAttribute("returnFee", returnFee);
            if (isAdmin()) {
                return "admin/rental-payment";
            }
            return "staff/rental-payment";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            if (isAdmin()) {
                return "redirect:/admin/returns/list";
            }
            return "redirect:/staff/returns/list";
        }
    }

    /**
     * UC15: Process rental payment
     */
    @PostMapping("/{contractId}/payment")
    public String processRentalPayment(
            @PathVariable Long contractId,
            @RequestParam("paymentMethod") String paymentMethodStr,
            RedirectAttributes redirectAttributes) {
        try {
            Contract contract = contractService.getContractById(contractId)
                    .orElseThrow(() -> new RuntimeException("Contract not found"));

            // Get return fees
            ReturnFee returnFee = returnService.getReturnFeeByContract(contractId)
                    .orElseThrow(() -> new RuntimeException("Return fees not found"));

            // Calculate total amount (rental fee + return fees)
            BigDecimal totalAmount = contract.getTotalRentalFee().add(returnFee.getTotalFees());

            // Parse payment method
            Payment.PaymentMethod paymentMethod = Payment.PaymentMethod.valueOf(paymentMethodStr);

            // Create rental payment
            paymentService.createRentalPayment(contractId, totalAmount, paymentMethod);

            redirectAttributes.addFlashAttribute("successMessage", "Thanh toán thành công!");
            if (isAdmin()) {
                return "redirect:/admin/contracts/" + contractId;
            }
            return "redirect:/staff/contracts/" + contractId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi thanh toán: " + e.getMessage());
            if (isAdmin()) {
                return "redirect:/admin/returns/" + contractId + "/payment";
            }
            return "redirect:/staff/returns/" + contractId + "/payment";
        }
    }

    /**
     * Get current authenticated user
     */
    private com.carrental.model.User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        String email = authentication.getName();
        return getUserByEmail(email);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }
}
