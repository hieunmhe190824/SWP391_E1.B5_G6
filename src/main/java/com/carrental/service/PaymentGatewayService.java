package com.carrental.service;

import com.carrental.config.PaymentGatewayConfig;
import com.carrental.model.Contract;
import com.carrental.model.Payment;
import com.carrental.repository.PaymentRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Service for handling online payment gateway integration
 * Manages payment initiation and processing for contract deposits
 */
@Service
public class PaymentGatewayService {

    @Autowired
    private PaymentGatewayConfig gatewayConfig;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ContractService contractService;

    /**
     * Initiate deposit payment for a contract
     * Creates a pending payment record and generates payment gateway URL
     * 
     * @param contractId Contract ID to pay deposit for
     * @param request HTTP request for building return URL
     * @return Payment URL to redirect customer to
     */
    @Transactional
    public String initiateDepositPayment(Long contractId, HttpServletRequest request) {
        // Get contract
        Contract contract = contractService.getContractById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        // Validate contract status
        if (contract.getStatus() != Contract.ContractStatus.PENDING_PAYMENT) {
            throw new RuntimeException("Contract is not in PENDING_PAYMENT status");
        }

        // Check if there's already a pending payment
        Optional<Payment> existingPayment = paymentRepository.findByContractAndStatus(
                contract, Payment.PaymentStatus.PENDING);
        if (existingPayment.isPresent()) {
            // Return existing payment URL if still valid
            Payment payment = existingPayment.get();
            if (payment.getPaymentUrl() != null && !payment.getPaymentUrl().isEmpty()) {
                return payment.getPaymentUrl();
            }
        }

        // Payment gateway parameters
        String version = gatewayConfig.getVersion();
        String command = "pay";
        String orderType = "other";

        // Amount in VND cents (multiply by 100 for payment gateway format)
        // Deposit amount is 50,000,000 VND
        long amount = contract.getDepositAmount().multiply(new BigDecimal("100")).longValue();

        // Generate unique transaction reference: DEPOSIT{contractId}_{random8digits}
        String transactionRef = "DEPOSIT" + contractId + "_" + PaymentGatewayConfig.getRandomNumber(8);
        String ipAddress = PaymentGatewayConfig.getIpAddress(request);
        String merchantCode = gatewayConfig.getMerchantCode();

        // Build payment gateway parameters
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", version);
        params.put("vnp_Command", command);
        params.put("vnp_TmnCode", merchantCode);
        params.put("vnp_Amount", String.valueOf(amount));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", transactionRef);
        params.put("vnp_OrderInfo", "Thanh toan coc hop dong #" + contractId);
        params.put("vnp_OrderType", orderType);
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", getReturnUrl(request));
        params.put("vnp_IpAddr", ipAddress);

        // Set expiry time (15 minutes)
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String createDate = formatter.format(calendar.getTime());
        params.put("vnp_CreateDate", createDate);

        calendar.add(Calendar.MINUTE, 15);
        String expireDate = formatter.format(calendar.getTime());
        params.put("vnp_ExpireDate", expireDate);

        // Build query string and hash
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        List<String> hashDataList = new ArrayList<>();
        List<String> queryList = new ArrayList<>();

        for (String fieldName : fieldNames) {
            String fieldValue = params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                try {
                    // Build hash data (URL encoded)
                    hashDataList.add(fieldName + "=" + URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                    // Build query (URL encoded)
                    queryList.add(URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString())
                            + "="
                            + URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));
                } catch (Exception e) {
                    throw new RuntimeException("Failed to encode payment parameters", e);
                }
            }
        }

        String hashData = String.join("&", hashDataList);
        String queryUrl = String.join("&", queryList);

        // Generate secure hash
        String secureHash = PaymentGatewayConfig.hmacSHA512(gatewayConfig.getSecretKey(), hashData);
        queryUrl += "&vnp_SecureHash=" + secureHash;
        String paymentUrl = gatewayConfig.getGatewayUrl() + "?" + queryUrl;

        // Create or update payment record
        Payment payment;
        if (existingPayment.isPresent()) {
            payment = existingPayment.get();
        } else {
            payment = new Payment();
            payment.setContract(contract);
            payment.setPaymentType(Payment.PaymentType.DEPOSIT);
            payment.setAmount(contract.getDepositAmount());
            payment.setPaymentMethod(Payment.PaymentMethod.ONLINE);
            payment.setStatus(Payment.PaymentStatus.PENDING);
        }

        payment.setTransactionRef(transactionRef);
        payment.setPaymentUrl(paymentUrl);
        paymentRepository.save(payment);

        return paymentUrl;
    }

    /**
     * Get return URL dynamically from request
     */
    private String getReturnUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();

        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);

        // Add port if not default
        if ((scheme.equals("http") && serverPort != 80) ||
                (scheme.equals("https") && serverPort != 443)) {
            url.append(":").append(serverPort);
        }

        url.append(contextPath).append("/payment/callback");
        return url.toString();
    }
}

