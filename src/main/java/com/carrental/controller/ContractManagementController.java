package com.carrental.controller;

import com.carrental.model.Contract;
import com.carrental.service.ContractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Contract Management Controller for Staff & Admin
 * Handles contract viewing and management for staff and admin
 */
@Controller
@RequestMapping({"/staff/contracts", "/admin/contracts"})
public class ContractManagementController {

    @Autowired
    private ContractService contractService;

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
     * View all contracts
     * GET /staff/contracts or /admin/contracts
     */
    @GetMapping
    public String listContracts(@RequestParam(required = false) String status,
                                @RequestParam(defaultValue = "0") int page,
                                Model model) {

        // Each page 6 items, sorted by createdAt desc
        Pageable pageable = PageRequest.of(page, 6, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Contract> contractsPage;

        if (status != null && !status.isEmpty()) {
            try {
                Contract.ContractStatus contractStatus = Contract.ContractStatus.valueOf(status.toUpperCase());
                contractsPage = contractService.getContractsByStatusPage(contractStatus, pageable);
            } catch (IllegalArgumentException e) {
                contractsPage = contractService.getContractsPage(pageable);
                status = null;
            }
        } else {
            contractsPage = contractService.getContractsPage(pageable);
        }

        model.addAttribute("contracts", contractsPage.getContent());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("currentPage", contractsPage.getNumber());
        model.addAttribute("totalPages", contractsPage.getTotalPages());

        // Choose view based on role
        if (isAdmin()) {
            return "admin/contracts-manage";
        }
        return "staff/contracts-manage";
    }
    
    /**
     * View pending payment contracts
     * GET /staff/contracts/pending-payment or /admin/contracts/pending-payment
     */
    @GetMapping("/pending-payment")
    public String pendingPaymentContracts(@RequestParam(defaultValue = "0") int page, Model model) {
        // Redirect to main list with status filter and page parameter so pagination logic is reused
        if (isAdmin()) {
            return "redirect:/admin/contracts?status=PENDING_PAYMENT&page=" + page;
        }
        return "redirect:/staff/contracts?status=PENDING_PAYMENT&page=" + page;
    }
    
    /**
     * View contract details
     * GET /staff/contracts/{id} or /admin/contracts/{id}
     */
    @GetMapping("/{id}")
    public String viewContractDetails(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Contract contract = contractService.getContractById(id)
                    .orElseThrow(() -> new RuntimeException("Contract not found"));
            model.addAttribute("contract", contract);

            if (isAdmin()) {
                return "admin/contract-detail";
            }
            return "staff/contract-detail";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            if (isAdmin()) {
                return "redirect:/admin/contracts";
            }
            return "redirect:/staff/contracts";
        }
    }
    
    /**
     * Export contract PDF
     * GET /staff/contracts/{id}/export or /admin/contracts/{id}/export
     */
    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportContractPdf(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Contract contract = contractService.getContractById(id)
                    .orElseThrow(() -> new RuntimeException("Contract not found"));
            
            byte[] pdfBytes = generateContractPdf(contract);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                    ContentDisposition.attachment()
                            .filename("contract-" + contract.getContractNumber() + ".pdf")
                            .build());
            
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Xuất PDF thất bại: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Build a simple PDF summary for contract
     */
    private byte[] generateContractPdf(Contract contract) throws DocumentException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, baos);
        document.open();

        Font titleFont = fontUnicode(16, Font.BOLD);
        Font sectionFont = fontUnicode(12, Font.BOLD);
        Font textFont = fontUnicode(11, Font.NORMAL);

        document.add(new Paragraph("HỢP ĐỒNG THUÊ XE", titleFont));
        document.add(new Paragraph("Mã hợp đồng: " + safe(contract.getContractNumber()), textFont));
        document.add(new Paragraph("Trạng thái: " + contract.getStatus(), textFont));
        document.add(new Paragraph("Ngày tạo: " + safe(contract.getCreatedAt()), textFont));
        document.add(Paragraph.getInstance("\n"));
        
        // Customer info table - use contract data instead of booking to avoid null issues
        document.add(new Paragraph("Thông tin khách hàng", sectionFont));
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        addRow(table, "Họ tên", safe(contract.getCustomer().getFullName()), textFont);
        addRow(table, "Email", safe(contract.getCustomer().getEmail()), textFont);
        addRow(table, "Số điện thoại", safe(contract.getCustomer().getPhone()), textFont);
        // Only show booking ID if booking exists
        if (contract.getBooking() != null) {
            addRow(table, "Mã đơn đặt", "#" + contract.getBooking().getId(), textFont);
        }
        addRow(table, "Thời gian thuê", safe(contract.getStartDate()) + " → " + safe(contract.getEndDate()), textFont);
        document.add(table);

        document.add(Paragraph.getInstance("\n"));
        document.add(new Paragraph("Thông tin xe", sectionFont));
        PdfPTable vehicleTable = new PdfPTable(2);
        vehicleTable.setWidthPercentage(100);
        addRow(vehicleTable, "Xe", safe(contract.getVehicle().getModel().getBrand().getBrandName()) + " " + safe(contract.getVehicle().getModel().getModelName()), textFont);
        addRow(vehicleTable, "Biển số", safe(contract.getVehicle().getLicensePlate()), textFont);
        document.add(vehicleTable);
        
        document.add(Paragraph.getInstance("\n"));
        document.add(new Paragraph("Thanh toán", sectionFont));
        PdfPTable paymentTable = new PdfPTable(2);
        paymentTable.setWidthPercentage(100);
        addRow(paymentTable, "Giá thuê / ngày", formatCurrency(contract.getDailyRate()), textFont);
        addRow(paymentTable, "Số ngày thuê", safe(contract.getTotalDays()) + " ngày", textFont);
        addRow(paymentTable, "Tiền cọc", formatCurrency(contract.getDepositAmount()), textFont);
        addRow(paymentTable, "Tổng tiền thuê", formatCurrency(contract.getTotalRentalFee()), textFont);
        document.add(paymentTable);
        
        document.close();
        return baos.toByteArray();
    }
    
    private void addRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell cell1 = new PdfPCell(new Phrase(label, font));
        PdfPCell cell2 = new PdfPCell(new Phrase(value, font));
        cell1.setBorderWidth(0.2f);
        cell2.setBorderWidth(0.2f);
        table.addCell(cell1);
        table.addCell(cell2);
    }
    
    private String safe(Object obj) {
        return obj == null ? "---" : obj.toString();
    }
    
    private String formatCurrency(java.math.BigDecimal amount) {
        if (amount == null) return "---";
        return String.format("%,.0f ₫", amount.doubleValue());
    }

    private Font fontUnicode(float size, int style) {
        // Register system fonts once per JVM (harmless if called multiple times)
        FontFactory.registerDirectories();

        // Try explicit Windows font files first (to ensure embedding Vietnamese glyphs)
        String[] winFonts = {
                "C:/Windows/Fonts/arial.ttf",
                "C:/Windows/Fonts/arialuni.ttf",
                "C:/Windows/Fonts/tahoma.ttf",
                "C:/Windows/Fonts/segoeui.ttf"
        };
        for (String path : winFonts) {
            try {
                BaseFont bf = BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                return new Font(bf, size, style);
            } catch (Exception ignore) {
                // try next
            }
        }

        // Try font names that may exist on system
        String[] names = {"Arial Unicode MS", "Segoe UI", "Tahoma", "Arial", "Times New Roman", "DejaVu Sans"};
        for (String name : names) {
            Font font = FontFactory.getFont(name, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, size, style);
            if (font != null && font.getBaseFont() != null) {
                return font;
            }
        }

        // Fallback
        return FontFactory.getFont(FontFactory.HELVETICA, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, size, style);
    }
    
    /**
     * Cancel contract (if needed)
     * POST /staff/contracts/{id}/cancel or /admin/contracts/{id}/cancel
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
        if (isAdmin()) {
            return "redirect:/admin/contracts/" + id;
        }
        return "redirect:/staff/contracts/" + id;
    }
}

