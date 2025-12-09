package com.carrental.controller;

import com.carrental.model.Contract;
import com.carrental.model.Payment;
import com.carrental.model.User;
import com.carrental.repository.UserRepository;
import com.carrental.service.ContractService;
import com.carrental.service.PaymentGatewayService;
import com.carrental.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Customer Contract Controller
 * Handles UC11 (View Contract) and deposit payment confirmation
 */
@Controller
@RequestMapping("/contracts")
public class CustomerContractController {

    @Autowired
    private ContractService contractService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentGatewayService paymentGatewayService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get current authenticated customer user
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * UC11: View customer's contracts
     * GET /contracts/my-contracts
     */
    @GetMapping("/my-contracts")
    public String myContracts(@RequestParam(required = false) String status, Model model) {
        User currentUser = getCurrentUser();
        List<Contract> contracts;
        
        if (status != null && !status.isEmpty()) {
            // Filter by status
            try {
                Contract.ContractStatus contractStatus = Contract.ContractStatus.valueOf(status.toUpperCase());
                contracts = contractService.getContractsByCustomer(currentUser.getId()).stream()
                        .filter(c -> c.getStatus() == contractStatus)
                        .toList();
            } catch (IllegalArgumentException e) {
                contracts = contractService.getContractsByCustomer(currentUser.getId());
            }
        } else {
            contracts = contractService.getContractsByCustomer(currentUser.getId());
        }
        
        model.addAttribute("contracts", contracts);
        model.addAttribute("selectedStatus", status);
        
        return "customer/contracts-list";
    }
    
    /**
     * UC11: View contract details
     * GET /contracts/{id}
     */
    @GetMapping("/{id}")
    public String viewContract(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            User currentUser = getCurrentUser();
            Contract contract = contractService.getContractById(id)
                    .orElseThrow(() -> new RuntimeException("Contract not found"));
            
            // Verify contract belongs to current user
            if (!contract.getCustomer().getId().equals(currentUser.getId())) {
                throw new RuntimeException("You don't have permission to view this contract");
            }
            
            // Get payment information if exists
            List<Payment> payments = paymentService.getPaymentsByContract(id);
            
            model.addAttribute("contract", contract);
            model.addAttribute("payments", payments);
            
            return "customer/contract-detail";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/contracts/my-contracts";
        }
    }
    
    /**
     * Show deposit payment page
     * GET /contracts/{id}/pay-deposit
     */
    @GetMapping("/{id}/pay-deposit")
    public String showPayDepositPage(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            User currentUser = getCurrentUser();
            Contract contract = contractService.getContractById(id)
                    .orElseThrow(() -> new RuntimeException("Contract not found"));
            
            // Verify contract belongs to current user
            if (!contract.getCustomer().getId().equals(currentUser.getId())) {
                throw new RuntimeException("You don't have permission to access this contract");
            }
            
            // Verify contract is in PENDING_PAYMENT status
            if (contract.getStatus() != Contract.ContractStatus.PENDING_PAYMENT) {
                throw new RuntimeException("This contract is not awaiting payment");
            }
            
            model.addAttribute("contract", contract);
            
            return "customer/contract-pay-deposit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/contracts/my-contracts";
        }
    }
    
    /**
     * Process deposit payment
     * POST /contracts/{id}/pay-deposit
     * Redirects to online payment gateway for ONLINE payment method
     * Processes directly for other payment methods (CASH, CARD, TRANSFER)
     */
    @PostMapping("/{id}/pay-deposit")
    public String payDeposit(@PathVariable Long id,
                            @RequestParam Payment.PaymentMethod paymentMethod,
                            HttpServletRequest request,
                            RedirectAttributes redirectAttributes) {
        try {
            User currentUser = getCurrentUser();

            // Check if payment method is ONLINE (payment gateway)
            if (paymentMethod == Payment.PaymentMethod.ONLINE) {
                // Initiate online payment gateway
                String paymentUrl = paymentGatewayService.initiateDepositPayment(id, request);

                // Redirect to payment gateway
                return "redirect:" + paymentUrl;
            } else {
                // Process direct payment (CASH, CARD, TRANSFER)
                paymentService.processDepositPayment(id, currentUser, paymentMethod);

                redirectAttributes.addFlashAttribute("successMessage",
                    "Thanh toán cọc thành công! Hợp đồng của bạn đã được kích hoạt.");

                return "redirect:/contracts/" + id;
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Lỗi khi thanh toán: " + e.getMessage());
            return "redirect:/contracts/" + id + "/pay-deposit";
        }
    }
}

