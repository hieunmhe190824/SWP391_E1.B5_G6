package com.carrental.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Service để gửi email
 */
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Gửi email chứa mật khẩu mới cho người dùng
     * @param toEmail Email người nhận
     * @param newPassword Mật khẩu mới
     */
    public void sendPasswordResetEmail(String toEmail, String newPassword) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Reset Mật Khẩu - Car Rental System");
            
            String emailContent = String.format(
                "Xin chào,\n\n" +
                "Bạn đã yêu cầu reset mật khẩu cho tài khoản Car Rental System.\n\n" +
                "Mật khẩu mới của bạn là: %s\n\n" +
                "Vui lòng đăng nhập và đổi mật khẩu ngay sau khi đăng nhập để bảo mật tài khoản.\n\n" +
                "Nếu bạn không yêu cầu reset mật khẩu, vui lòng liên hệ với chúng tôi ngay lập tức.\n\n" +
                "Trân trọng,\n" +
                "Car Rental System Team",
                newPassword
            );
            
            message.setText(emailContent);
            mailSender.send(message);
            
            System.out.println("Email reset password đã được gửi đến: " + toEmail);
        } catch (Exception e) {
            System.err.println("Lỗi khi gửi email: " + e.getMessage());
            throw new RuntimeException("Không thể gửi email. Vui lòng thử lại sau.");
        }
    }
}
