package com.carrental.controller;

import com.carrental.model.*;
import com.carrental.repository.LocationRepository;
import com.carrental.repository.UserRepository;
import com.carrental.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
@RequestMapping("/staff/returns")
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
     * UC14: Show list of contracts ready for return
     */
    @GetMapping("/list")
    public String showReturnList(Model model) {
        List<Contract> contractsReadyForReturn = returnService.getContractsReadyForReturn();
        model.addAttribute("contracts", contractsReadyForReturn);
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
            return "staff/return-form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
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
            return "redirect:/staff/returns/list";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xác thực: " + e.getMessage());
            return "redirect:/staff/returns/" + contractId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi trả xe: " + e.getMessage());
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

            // Verify contract is completed
            if (contract.getStatus() != Contract.ContractStatus.COMPLETED) {
                redirectAttributes.addFlashAttribute("errorMessage", "Contract is not completed yet");
                return "redirect:/staff/returns/list";
            }

            // Check if payment already done
            if (paymentService.getRentalPayment(contractId).isPresent()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Rental payment already processed");
                return "redirect:/staff/contracts/" + contractId;
            }

            // Get return fees
            ReturnFee returnFee = returnService.getReturnFeeByContract(contractId)
                    .orElse(null);

            model.addAttribute("contract", contract);
            model.addAttribute("returnFee", returnFee);
            return "staff/rental-payment";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
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
            return "redirect:/staff/contracts/" + contractId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi thanh toán: " + e.getMessage());
            return "redirect:/staff/returns/" + contractId + "/payment";
        }
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }
}
