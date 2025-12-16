package com.carrental.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Properties;

/**
 * Service để gửi email sử dụng Gmail API với Web Application OAuth 2.0
 */
@Service
public class GmailApiService {

    private static final String APPLICATION_NAME = "Car Rental System";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    @Autowired
    private TokenStorageService tokenStorageService;

    @Value("${gmail.user.email}")
    private String userEmail;

    /**
     * Tạo Gmail service instance với access token
     */
    private Gmail getGmailService() throws Exception {
        String accessToken = tokenStorageService.getToken(userEmail);
        
        if (accessToken == null) {
            throw new RuntimeException("Chưa authorize Gmail API. Vui lòng truy cập /oauth2/authorize để authorize.");
        }
        
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        
        GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);
        
        return new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /**
     * Kiểm tra xem đã authorize chưa
     */
    public boolean isAuthorized() {
        return tokenStorageService.hasToken(userEmail);
    }

    /**
     * Gửi email chứa mật khẩu mới cho người dùng
     * @param toEmail Email người nhận
     * @param newPassword Mật khẩu mới
     */
    public void sendPasswordResetEmail(String toEmail, String newPassword) {
        try {
            if (!isAuthorized()) {
                throw new RuntimeException("Chưa authorize Gmail API. Admin cần truy cập /oauth2/authorize để cấp quyền.");
            }
            
            Gmail service = getGmailService();
            MimeMessage email = createEmail(toEmail, userEmail, 
                "Reset Mật Khẩu - Car Rental System", 
                createEmailBody(newPassword));
            sendMessage(service, "me", email);
            System.out.println("Email reset password đã được gửi đến: " + toEmail);
        } catch (Exception e) {
            System.err.println("Lỗi khi gửi email: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Không thể gửi email. " + e.getMessage());
        }
    }

    /**
     * Tạo MimeMessage email
     */
    private MimeMessage createEmail(String to, String from, String subject, String bodyText) throws Exception {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(from));
        email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
        email.setSubject(subject);
        email.setText(bodyText, "UTF-8");
        return email;
    }

    /**
     * Gửi email qua Gmail API
     */
    private Message sendMessage(Gmail service, String userId, MimeMessage emailContent) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.getUrlEncoder().encodeToString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return service.users().messages().send(userId, message).execute();
    }

    /**
     * Tạo nội dung email
     */
    private String createEmailBody(String newPassword) {
        return String.format(
            "Xin chào,\n\n" +
            "Bạn đã yêu cầu reset mật khẩu cho tài khoản Car Rental System.\n\n" +
            "Mật khẩu mới của bạn là: %s\n\n" +
            "Vui lòng đăng nhập và đổi mật khẩu ngay sau khi đăng nhập để bảo mật tài khoản.\n\n" +
            "Nếu bạn không yêu cầu reset mật khẩu, vui lòng liên hệ với chúng tôi ngay lập tức.\n\n" +
            "Trân trọng,\n" +
            "Car Rental System Team",
            newPassword
        );
    }
}
