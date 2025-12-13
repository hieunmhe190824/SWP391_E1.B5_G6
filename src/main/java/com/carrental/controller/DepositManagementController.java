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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/staff/deposits")
public class DepositManagementController {

    @Autowired
    private DepositService depositService;

    @Autowired
    private TrafficViolationService violationService;

    @Autowired
    private RefundService refundService;

    /**
     * UC16, UC17, UC18: List all deposit holds
     */
    @GetMapping("/holds")
    public String listDepositHolds(
            @RequestParam(value = "status", required = false) String statusStr,
            Model model) {
        
        List<DepositHold> deposits;
        
        if (statusStr != null && !statusStr.isEmpty()) {
            DepositHold.DepositStatus status = DepositHold.DepositStatus.valueOf(statusStr);
            deposits = depositService.getDepositsByStatus(status);
        } else {
            deposits = depositService.getAllDeposits();
        }

        model.addAttribute("deposits", deposits);
        model.addAttribute("selectedStatus", statusStr);
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
            
            return "staff/deposit-detail";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
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
            return "redirect:/staff/deposits/" + holdId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi thêm vi phạm: " + e.getMessage());
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
            return "redirect:/staff/deposits/" + holdId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa vi phạm: " + e.getMessage());
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
                return "redirect:/staff/deposits/" + holdId;
            }

            // Check if refund already processed
            if (refundService.getRefundByDepositHold(holdId).isPresent()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Refund already processed");
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
            
            return "staff/refund-process";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
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
            // Parse refund method
            RefundMethod refundMethod = RefundMethod.valueOf(refundMethodStr);

            // Process refund
            Refund refund = refundService.processRefund(holdId, refundMethod);

            redirectAttributes.addFlashAttribute("successMessage", 
                String.format("Hoàn tiền thành công! Số tiền: %,.0f VNĐ", refund.getRefundAmount()));
            return "redirect:/staff/deposits/" + holdId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi hoàn tiền: " + e.getMessage());
            return "redirect:/staff/deposits/" + holdId + "/refund";
        }
    }
}
