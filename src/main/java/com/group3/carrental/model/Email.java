package com.group3.carrental.model;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

import com.group3.carrental.security.EnvReader;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

public class Email {
    // Source - https://stackoverflow.com/a
    // Posted by RichieHindle, modified by community. See post 'Timeline' for change history
    // Retrieved 2025-11-30, License - CC BY-SA 4.0

    // Set up the SMTP server.
    private Email() {
        //default cons
    }
    public static void sendEmail(String recipient, String recipientName) throws MessagingException, UnsupportedEncodingException {

        // ==== 1. Configure session =================================================
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new jakarta.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("khongtenten11@gmail.com", EnvReader.load("APP_PASSWORD"));
            }
        });
        // ==== 2. Build the message =================================================
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress("noreply@easycarrental.com", "Easycar"));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
        message.setSubject("Password Reset Request");

        // ---- Placeholders you will replace at runtime -----------------------------
        String resetUrl = "https://acmecorp.com/reset?token=abc123xyz"; // real URL
        String operatingSystem = "Windows 10";
        String browser = "Chrome 129";

        // ---- HTML body (exactly like your screenshot) -----------------------------
        String html = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <style>
                body {font-family: Arial, sans-serif; background:#f6f6f6; margin:0;}
                .container {max-width:600px; margin:30px auto; background:white; border:1px solid #ddd;}
                .header {padding:20px; text-align:center; border-bottom:1px solid #eee;}
                .content {padding:40px; line-height:1.6; color:#333;}
                .button {
                    display:inline-block;
                    background:#28a745;
                    color:white !important;
                    padding:14px 32px;
                    text-decoration:none;
                    border-radius:4px;
                    font-weight:bold;
                }
                .security {margin-top:40px; font-size:14px; color:#555;}
                .footer {padding:20px; text-align:center; font-size:12px; color:#888;}
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header"><h1>Easycar</h1></div>
                <div class="content">
                    <h2>Hi %s,</h2>
                    <p>You recently requested to reset your password for your <strong>Easycar</strong> account.
                    Use the button below to reset it. <strong>This password reset is only valid for the next 24 hours.</strong></p>

                    <p style="text-align:center;">
                        <a href="%s" class="button">Reset your password</a>
                    </p>

                    <div class="security">
                        For security, this request was received from a <strong>%s</strong> device using <strong>%s</strong>.<br>
                        If you did not request a password reset, please ignore this email or <a href="mailto:support@acmecorp.com">contact support</a>.
                    </div>

                    <p>Thanks,<br><strong>The Easycar Team</strong></p>

                    <hr style="border:none; border-top:1px solid #eee; margin:40px 0;">
                    <p style="font-size:12px; color:#999;">
                        Trouble clicking? Copy and paste this link:<br>
                        <a href="%s">%s</a>
                    </p>
                </div>

                <div class="footer">
                    © 2025 ACME Product, All rights reserved.<br>
                    ACME Company Name, LLC • 1234 Street Rd. • Suite 1234
                </div>
            </div>
        </body>
        </html>
        """.formatted(recipientName, resetUrl, operatingSystem, browser, resetUrl, resetUrl);

        // This ONE line is all you need for HTML-only
        message.setContent(html, "text/html; charset=utf-8");

        Transport.send(message);
    }
}


