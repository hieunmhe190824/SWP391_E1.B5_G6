package com.carrental.controller;

import com.carrental.model.Contract;
import com.carrental.model.User;
import com.carrental.repository.UserRepository;
import com.carrental.service.ContractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Contract Management Controller for Staff
 * Handles contract viewing and management for staff
 */
@Controller
@RequestMapping("/staff/contracts")
public class ContractManagementController {

    @Autowired
    private ContractService contractService;
    
    @Autowired
    private UserRepository userRepository;

    /**
     * View all contracts (Staff)
     * GET /staff/contracts
     */
    @GetMapping
    public String listContracts(@RequestParam(required = false) String status, Model model) {
        List<Contract> contracts;
        
        if (status != null && !status.isEmpty()) {
            // Filter by status
            try {
                Contract.ContractStatus contractStatus = Contract.ContractStatus.valueOf(status.toUpperCase());
                contracts = contractService.getContractsByStatus(contractStatus);
            } catch (IllegalArgumentException e) {
                contracts = contractService.getAllContracts();
            }
        } else {
            contracts = contractService.getAllContracts();
        }
        
        model.addAttribute("contracts", contracts);
        model.addAttribute("selectedStatus", status);
        
        return "staff/contracts-manage";
    }
    
    /**
     * View pending payment contracts
     * GET /staff/contracts/pending-payment
     */
    @GetMapping("/pending-payment")
    public String pendingPaymentContracts(Model model) {
        List<Contract> pendingContracts = contractService.getContractsByStatus(Contract.ContractStatus.PENDING_PAYMENT);
        model.addAttribute("contracts", pendingContracts);
        model.addAttribute("selectedStatus", "PENDING_PAYMENT");
        return "staff/contracts-manage";
    }
    
    /**
     * View contract details
     * GET /staff/contracts/{id}
     */
    @GetMapping("/{id}")
    public String viewContractDetails(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Contract contract = contractService.getContractById(id)
                    .orElseThrow(() -> new RuntimeException("Contract not found"));
            
            model.addAttribute("contract", contract);
            
            return "staff/contract-detail";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/staff/contracts";
        }
    }
    
    /**
     * Cancel contract (if needed)
     * POST /staff/contracts/{id}/cancel
     */
    @PostMapping("/{id}/cancel")
    public String cancelContract(@PathVariable Long id, 
                                 @RequestParam(required = false) String reason,
                                 RedirectAttributes redirectAttributes) {
        try {
            contractService.updateContractStatus(id, Contract.ContractStatus.CANCELLED);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Đã hủy hợp đồng #" + id);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Lỗi khi hủy hợp đồng: " + e.getMessage());
        }
        
        return "redirect:/staff/contracts/" + id;
    }
}

