package com.carrental.controller;

import com.carrental.model.Contract;
import com.carrental.model.Payment;
import com.carrental.model.User;
import com.carrental.repository.UserRepository;
import com.carrental.service.ContractService;
import com.carrental.service.PaymentGatewayService;
import com.carrental.service.PaymentService;
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
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayOutputStream;
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
                            HttpServletRequest request,
                            RedirectAttributes redirectAttributes) {
        try {
            User currentUser = getCurrentUser();

            // Always route through online payment gateway
            String paymentUrl = paymentGatewayService.initiateDepositPayment(id, request);
            return "redirect:" + paymentUrl;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Lỗi khi thanh toán: " + e.getMessage());
            return "redirect:/contracts/" + id + "/pay-deposit";
        }
    }

    /**
     * Export contract PDF for customer (only their own contract)
     * GET /contracts/{id}/export
     */
    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportContractPdf(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            User currentUser = getCurrentUser();
            Contract contract = contractService.getContractById(id)
                    .orElseThrow(() -> new RuntimeException("Contract not found"));

            if (!contract.getCustomer().getId().equals(currentUser.getId())) {
                throw new RuntimeException("You don't have permission to export this contract");
            }

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
     * Build PDF for contract (minimal summary)
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
        FontFactory.registerDirectories();

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

        String[] names = {"Arial Unicode MS", "Segoe UI", "Tahoma", "Arial", "Times New Roman", "DejaVu Sans"};
        for (String name : names) {
            Font font = FontFactory.getFont(name, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, size, style);
            if (font != null && font.getBaseFont() != null) {
                return font;
            }
        }

        return FontFactory.getFont(FontFactory.HELVETICA, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, size, style);
    }
}

