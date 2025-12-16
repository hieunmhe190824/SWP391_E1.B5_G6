package com.carrental.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.GmailScopes;
import com.carrental.service.TokenStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;

/**
 * Controller xử lý OAuth 2.0 callback từ Google
 */
@Controller
public class OAuth2CallbackController {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_SEND);

    @Autowired
    private TokenStorageService tokenStorageService;

    @Value("${gmail.credentials.file.path:/credentials.json}")
    private String credentialsFilePath;

    @Value("${gmail.user.email}")
    private String userEmail;

    /**
     * Endpoint để bắt đầu OAuth flow
     * URL: /oauth2/authorize
     */
    @GetMapping("/oauth2/authorize")
    public String authorize() {
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            GoogleAuthorizationCodeFlow flow = getFlow(HTTP_TRANSPORT);
            
            String authorizationUrl = flow.newAuthorizationUrl()
                    .setRedirectUri("http://localhost:8080/oauth2/callback")
                    .build();
            
            return "redirect:" + authorizationUrl;
        } catch (Exception e) {
            System.err.println("Error creating authorization URL: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/auth/login?error=oauth_failed";
        }
    }

    /**
     * Endpoint nhận callback từ Google sau khi user authorize
     * URL: /oauth2/callback?code=...
     */
    @GetMapping("/oauth2/callback")
    public String handleCallback(@RequestParam("code") String code) {
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            GoogleAuthorizationCodeFlow flow = getFlow(HTTP_TRANSPORT);
            
            // Exchange authorization code for access token
            GoogleTokenResponse tokenResponse = flow.newTokenRequest(code)
                    .setRedirectUri("http://localhost:8080/oauth2/callback")
                    .execute();
            
            // Lưu access token
            String accessToken = tokenResponse.getAccessToken();
            tokenStorageService.saveToken(userEmail, accessToken);
            
            System.out.println("OAuth authorization successful! Token saved for: " + userEmail);
            
            return "redirect:/auth/login?oauth=success";
        } catch (Exception e) {
            System.err.println("Error handling OAuth callback: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/auth/login?error=oauth_callback_failed";
        }
    }

    /**
     * Tạo GoogleAuthorizationCodeFlow
     */
    private GoogleAuthorizationCodeFlow getFlow(NetHttpTransport httpTransport) throws Exception {
        InputStream in = getClass().getResourceAsStream(credentialsFilePath);
        if (in == null) {
            throw new FileNotFoundException("Không tìm thấy file credentials: " + credentialsFilePath);
        }
        
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        
        return new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                .setAccessType("offline")
                .build();
    }
}
