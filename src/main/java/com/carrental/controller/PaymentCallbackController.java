package com.carrental.controller;

import com.carrental.config.PaymentGatewayConfig;
import com.carrental.model.Contract;
import com.carrental.model.Payment;
import com.carrental.repository.PaymentRepository;
import com.carrental.service.ContractService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Controller for handling payment gateway callback
 * Processes payment results and updates contract status
 */
@Controller
@RequestMapping("/payment")
public class PaymentCallbackController {

    @Autowired
    private PaymentGatewayConfig gatewayConfig;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ContractService contractService;

    /**
     * Handle payment gateway return callback
     * Validates payment signature and updates payment/contract status
     */
    @GetMapping("/callback")
    public String handlePaymentCallback(HttpServletRequest request, Model model) {
        // Get all parameters from payment gateway
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements(); ) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // URL encode both field name and value for hash verification
                try {
                    String encodedName = URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString());
                    String encodedValue = URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString());
                    fields.put(encodedName, encodedValue);
                } catch (Exception e) {
                    System.err.println("ERROR: Failed to encode field " + fieldName + ": " + e.getMessage());
                    fields.put(fieldName, fieldValue); // Fallback to non-encoded
                }
            }
        }

        // Get secure hash from payment gateway (original, not encoded)
        String secureHash = request.getParameter("vnp_SecureHash");

        // Remove hash fields before validating
        try {
            String encodedHashType = URLEncoder.encode("vnp_SecureHashType", StandardCharsets.UTF_8.toString());
            String encodedHash = URLEncoder.encode("vnp_SecureHash", StandardCharsets.UTF_8.toString());
            fields.remove(encodedHashType);
            fields.remove(encodedHash);
        } catch (Exception e) {
            // Fallback to non-encoded keys
            fields.remove("vnp_SecureHashType");
            fields.remove("vnp_SecureHash");
        }

        // Validate signature
        String signValue = gatewayConfig.hashAllFields(fields);
        boolean isValidSignature = signValue.equals(secureHash);

        // Get payment details
        String transactionRef = request.getParameter("vnp_TxnRef");
        String amount = request.getParameter("vnp_Amount");
        String orderInfo = request.getParameter("vnp_OrderInfo");
        String responseCode = request.getParameter("vnp_ResponseCode");
        String transactionNo = request.getParameter("vnp_TransactionNo");
        String bankCode = request.getParameter("vnp_BankCode");
        String payDate = request.getParameter("vnp_PayDate");
        String transactionStatus = request.getParameter("vnp_TransactionStatus");
        String cardType = request.getParameter("vnp_CardType");

        // Set attributes for view
        model.addAttribute("transactionRef", transactionRef);
        model.addAttribute("amount", amount);
        model.addAttribute("orderInfo", orderInfo);
        model.addAttribute("responseCode", responseCode);
        model.addAttribute("transactionNo", transactionNo);
        model.addAttribute("bankCode", bankCode);
        model.addAttribute("payDate", payDate);
        model.addAttribute("transactionStatus", transactionStatus);
        model.addAttribute("cardType", cardType);
        model.addAttribute("isValidSignature", isValidSignature);

        // Process payment result
        String paymentStatus = "failed";
        String message = "";
        Long contractId = null;

        try {
            if (isValidSignature) {
                // Find payment by transaction reference
                Optional<Payment> paymentOpt = paymentRepository.findByTransactionRef(transactionRef);
                if (!paymentOpt.isPresent()) {
                    message = "Payment record not found";
                    paymentStatus = "failed";
                } else {
                    Payment payment = paymentOpt.get();
                    contractId = payment.getContract().getId();

                    // Check transaction status
                    if ("00".equals(responseCode) && "00".equals(transactionStatus)) {
                        // Payment successful
                        paymentStatus = "success";
                        message = "Thanh toán cọc thành công! Hợp đồng của bạn đã được kích hoạt.";

                        // Update payment record
                        payment.setStatus(Payment.PaymentStatus.COMPLETED);
                        payment.setPaymentDate(parsePaymentDate(payDate));
                        payment.setGatewayTransactionId(transactionNo);
                        payment.setGatewayResponseCode(responseCode);
                        payment.setGatewayTransactionStatus(transactionStatus);
                        payment.setGatewayBankCode(bankCode);
                        payment.setGatewayCardType(cardType);
                        payment.setGatewayPayDate(payDate);
                        payment.setGatewaySecureHash(secureHash);
                        paymentRepository.save(payment);

                        // Update contract status to ACTIVE
                        contractService.updateContractStatus(contractId, Contract.ContractStatus.ACTIVE);

                    } else {
                        // Payment failed
                        paymentStatus = "failed";
                        message = "Thanh toán thất bại: " + getErrorMessage(responseCode);

                        // Update payment record
                        payment.setStatus(Payment.PaymentStatus.FAILED);
                        payment.setGatewayResponseCode(responseCode);
                        payment.setGatewayTransactionStatus(transactionStatus);
                        payment.setGatewayBankCode(bankCode);
                        payment.setGatewayCardType(cardType);
                        payment.setGatewayPayDate(payDate);
                        payment.setGatewaySecureHash(secureHash);
                        paymentRepository.save(payment);
                    }
                }
            } else {
                // Invalid signature
                paymentStatus = "failed";
                message = "Chữ ký thanh toán không hợp lệ. Giao dịch có thể bị giả mạo. Vui lòng liên hệ hỗ trợ.";
            }
        } catch (Exception e) {
            System.err.println("ERROR: Payment callback processing failed: " + e.getMessage());
            e.printStackTrace();
            message = "Lỗi xử lý thanh toán: " + e.getMessage();
            paymentStatus = "failed";
        }

        model.addAttribute("paymentStatus", paymentStatus);
        model.addAttribute("message", message);
        model.addAttribute("contractId", contractId);

        return "customer/payment-result";
    }

    /**
     * Parse payment date from gateway format (yyyyMMddHHmmss) to LocalDateTime
     */
    private LocalDateTime parsePaymentDate(String payDate) {
        if (payDate == null || payDate.isEmpty()) {
            return LocalDateTime.now();
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            return LocalDateTime.parse(payDate, formatter);
        } catch (Exception e) {
            System.err.println("ERROR: Failed to parse payment date: " + payDate);
            return LocalDateTime.now();
        }
    }

    /**
     * Get friendly error message for payment gateway response codes
     */
    private String getErrorMessage(String responseCode) {
        if (responseCode == null) return "Lỗi không xác định";

        switch (responseCode) {
            case "07":
                return "Giao dịch thành công nhưng yêu cầu xác nhận bị ngân hàng từ chối";
            case "09":
                return "Thẻ/Tài khoản chưa đăng ký dịch vụ Internet Banking";
            case "10":
                return "Khách hàng xác thực thông tin thẻ/tài khoản không đúng quá 3 lần";
            case "11":
                return "Giao dịch hết hạn. Vui lòng thử lại";
            case "12":
                return "Thẻ/Tài khoản bị khóa";
            case "13":
                return "Mã OTP không đúng. Vui lòng thử lại";
            case "24":
                return "Giao dịch bị hủy bởi người dùng";
            case "51":
                return "Tài khoản không đủ số dư";
            case "65":
                return "Vượt quá hạn mức giao dịch";
            case "75":
                return "Cổng thanh toán đang bảo trì";
            case "79":
                return "Giao dịch hết hạn, vui lòng thử lại";
            default:
                return "Giao dịch thất bại (Mã lỗi: " + responseCode + ")";
        }
    }
}

