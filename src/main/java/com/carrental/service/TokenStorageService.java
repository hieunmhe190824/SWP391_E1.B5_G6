package com.carrental.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service để lưu trữ OAuth tokens trong memory
 * Trong production, nên lưu vào database hoặc Redis
 */
@Service
public class TokenStorageService {

    // Lưu token theo user email
    private final Map<String, String> tokenStore = new ConcurrentHashMap<>();
    
    /**
     * Lưu access token cho user
     */
    public void saveToken(String userEmail, String accessToken) {
        tokenStore.put(userEmail, accessToken);
    }
    
    /**
     * Lấy access token của user
     */
    public String getToken(String userEmail) {
        return tokenStore.get(userEmail);
    }
    
    /**
     * Xóa token của user
     */
    public void removeToken(String userEmail) {
        tokenStore.remove(userEmail);
    }
    
    /**
     * Kiểm tra user đã có token chưa
     */
    public boolean hasToken(String userEmail) {
        return tokenStore.containsKey(userEmail) && tokenStore.get(userEmail) != null;
    }
}
