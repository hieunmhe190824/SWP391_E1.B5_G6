package com.carrental.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

/**
 * Custom Authentication Success Handler
 * Redirect user dựa trên role sau khi đăng nhập thành công
 */
@Component
public class CustomAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                       HttpServletResponse response, 
                                       Authentication authentication) throws IOException, ServletException {
        
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        String redirectUrl = "/"; // Default redirect
        
        // Kiểm tra role của user và redirect đến trang phù hợp
        for (GrantedAuthority authority : authorities) {
            String role = authority.getAuthority();
            
            if (role.equals("ROLE_ADMIN")) {
                // Admin redirect đến Admin Dashboard
                redirectUrl = "/admin/dashboard";
                break;
            } else if (role.equals("ROLE_STAFF")) {
                // Staff redirect đến Staff Dashboard
                redirectUrl = "/staff/dashboard";
                break;
            } else if (role.equals("ROLE_CUSTOMER")) {
                // Customer redirect đến trang home
                redirectUrl = "/";
                break;
            }
        }
        
        // Redirect đến URL phù hợp
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}

