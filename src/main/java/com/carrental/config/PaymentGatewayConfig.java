package com.carrental.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * Payment Gateway Configuration and Utility Class
 * Handles online payment processing integration
 */
@Configuration
public class PaymentGatewayConfig {

    // Payment Gateway Configuration
    // VNPay Sandbox credentials - Replace with production credentials for live environment
    @Value("${payment.gateway.url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String gatewayUrl;

    @Value("${payment.gateway.merchant.code:4YUP19I4}")
    private String merchantCode;

    @Value("${payment.gateway.secret.key:MDUIFDCRAKLNBPOFIAFNEKFRNMFBYEPX}")
    private String secretKey;

    @Value("${payment.gateway.api.url:https://sandbox.vnpayment.vn/merchant_webapi/api/transaction}")
    private String apiUrl;

    @Value("${payment.gateway.version:2.1.0}")
    private String version;

    // Getters for configuration values
    public String getGatewayUrl() {
        return gatewayUrl;
    }

    public String getMerchantCode() {
        return merchantCode;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public String getVersion() {
        return version;
    }

    /**
     * Generate MD5 hash
     */
    public static String md5(String message) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(message.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            return "";
        }
    }

    /**
     * Generate SHA256 hash
     */
    public static String sha256(String message) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(message.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            return "";
        }
    }

    /**
     * Generate HMAC SHA512 signature
     */
    public static String hmacSHA512(final String key, final String data) {
        try {
            if (key == null || data == null) {
                throw new NullPointerException("Key and data cannot be null");
            }
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes(StandardCharsets.UTF_8);
            final SecretKeySpec secretKeySpec = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKeySpec);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            return "";
        }
    }

    /**
     * Build the canonical hash data string from fields (sorted & URL encoded)
     * This matches VNPay's expected signature input.
     */
    public String buildHashData(Map<String, String> fields) {
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        List<String> hashDataList = new ArrayList<>();

        for (String fieldName : fieldNames) {
            String fieldValue = fields.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                try {
                    // VNPay requires hashing over URL-encoded values in the same way they were sent
                    String encodedValue = URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString());
                    hashDataList.add(fieldName + "=" + encodedValue);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to encode payment field for hashing", e);
                }
            }
        }

        return String.join("&", hashDataList);
    }

    /**
     * Hash all fields for payment gateway signature
     * NOTE: Fields should already be URL encoded before passing to this method
     */
    public String hashAllFields(Map<String, String> fields) {
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        List<String> hashDataList = new ArrayList<>();

        for (String fieldName : fieldNames) {
            String fieldValue = fields.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Fields are already URL encoded, just concatenate
                hashDataList.add(fieldName + "=" + fieldValue);
            }
        }

        String hashData = String.join("&", hashDataList);
        System.out.println("DEBUG hashAllFields - Hash Data: " + hashData);
        String hash = hmacSHA512(secretKey, hashData);
        System.out.println("DEBUG hashAllFields - Generated Hash: " + hash);
        return hash;
    }

    /**
     * Get client IP address from request
     */
    public static String getIpAddress(HttpServletRequest request) {
        String ipAddress;
        try {
            ipAddress = request.getHeader("X-FORWARDED-FOR");
            if (ipAddress == null || ipAddress.isEmpty()) {
                ipAddress = request.getRemoteAddr();
            }
        } catch (Exception e) {
            ipAddress = "Invalid IP:" + e.getMessage();
        }
        return ipAddress;
    }

    /**
     * Generate random number string
     */
    public static String getRandomNumber(int len) {
        Random rnd = new Random();
        String chars = "0123456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }
}

