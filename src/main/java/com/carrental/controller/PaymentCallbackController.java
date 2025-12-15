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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(PaymentCallbackController.class);

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
        log.info("Received VNPay callback with raw params: {}", Collections.list(request.getParameterNames())
                .stream()
                .collect(HashMap::new, (m, k) -> m.put(k, request.getParameter(k)), HashMap::putAll));

        // Get all parameters from VNPay
        // IMPORTANT: For hash verification, only field VALUES must be URL encoded (per VNPay spec)
        // Field names remain as-is (not encoded in hash data, only in query URL)
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements(); ) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // For hash data: fieldName stays as-is, only encode the value
                try {
                    String encodedValue = java.net.URLEncoder.encode(fieldValue, java.nio.charset.StandardCharsets.UTF_8.toString());
                    fields.put(fieldName, encodedValue);
                } catch (Exception e) {
                    log.error("Failed to encode field {} for hashing", fieldName, e);
                    fields.put(fieldName, fieldValue); // Fallback to non-encoded
                }
            }
        }

        // Get secure hash from VNPay (original, not encoded)
        String secureHash = request.getParameter("vnp_SecureHash");

        // Remove hash fields before validating (using non-encoded keys since field names are not encoded)
        fields.remove("vnp_SecureHashType");
        fields.remove("vnp_SecureHash");

        // Validate signature using the same method as in VNPayConfig.hashAllFields
        String signValue = gatewayConfig.hashAllFields(fields);
        boolean isValidSignature = signValue.equalsIgnoreCase(secureHash);

        // Debug logs to compare hash inputs with VNPay (helps diagnose signature mismatches)
        log.info("[VNPay] Callback computedSign={}, receivedHash={}, isValidSignature={}", signValue, secureHash, isValidSignature);

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
        // Parse amount from gateway (amount is in cents)
        java.math.BigDecimal amountVnd = java.math.BigDecimal.ZERO;
        try {
            if (amount != null && !amount.isEmpty()) {
                amountVnd = new java.math.BigDecimal(amount).divide(new java.math.BigDecimal("100"));
            }
        } catch (Exception ex) {
            log.error("Failed to parse amount {}", amount, ex);
        }

        model.addAttribute("amount", amount);
        model.addAttribute("amountVnd", amountVnd);
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
                    log.warn("Payment callback received but no payment found for transactionRef={}", transactionRef);
                    message = "Payment record not found";
                    paymentStatus = "failed";
                } else {
                    Payment payment = paymentOpt.get();
                    contractId = payment.getContract().getId();
                    log.info("Processing callback for contractId={}, transactionRef={}, responseCode={}, transactionStatus={}",
                            contractId, transactionRef, responseCode, transactionStatus);

                    // Check transaction status
                    if ("00".equals(responseCode) && "00".equals(transactionStatus)) {
                        // Payment successful
                        paymentStatus = "success";
                        boolean isDeposit = payment.getPaymentType() == Payment.PaymentType.DEPOSIT;
                        if (isDeposit) {
                            message = "Thanh toán cọc thành công! Hợp đồng của bạn đã được kích hoạt.";
                        } else {
                            message = "Thanh toán hóa đơn thành công! Cảm ơn bạn.";
                        }

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
                        log.info("Payment completed for contractId={}, vnPayTxn={}, bankCode={}", contractId, transactionNo, bankCode);

                        // Update contract status based on payment type
                        if (isDeposit) {
                            contractService.updateContractStatus(contractId, Contract.ContractStatus.ACTIVE);
                        } else if (payment.getPaymentType() == Payment.PaymentType.RENTAL) {
                            contractService.updateContractStatus(contractId, Contract.ContractStatus.COMPLETED);
                        }

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
                        log.warn("Payment failed for contractId={}, responseCode={}, transactionStatus={}, message={}",
                                contractId, responseCode, transactionStatus, message);
                    }
                }
            } else {
                // Invalid signature
                paymentStatus = "failed";
                message = "Chữ ký thanh toán không hợp lệ. Giao dịch có thể bị giả mạo. Vui lòng liên hệ hỗ trợ.";
                log.error("Invalid VNPay signature for transactionRef={}, receivedHash={}, computed={}", transactionRef, secureHash, signValue);
                // Try to mark payment as failed if we have the reference
                if (transactionRef != null) {
                    paymentRepository.findByTransactionRef(transactionRef).ifPresent(p -> {
                        p.setStatus(Payment.PaymentStatus.FAILED);
                        paymentRepository.save(p);
                    });
                }
            }
        } catch (Exception e) {
            log.error("Payment callback processing failed for transactionRef={}", transactionRef, e);
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
            log.warn("Failed to parse payment date {}, fallback to now()", payDate, e);
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

