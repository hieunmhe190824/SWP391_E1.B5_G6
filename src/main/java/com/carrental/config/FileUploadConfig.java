package com.carrental.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration for file upload and static resource handling
 * UC05: Manage Vehicles - Image Upload Configuration
 *
 * Note: File upload limits are configured in application.properties:
 * - spring.servlet.multipart.max-file-size=5MB
 * - spring.servlet.multipart.max-request-size=25MB
 */
@Configuration
public class FileUploadConfig implements WebMvcConfigurer {

    private static final String UPLOAD_DIR = "uploads";

    /**
     * Configure static resource handlers
     * Map /images/** to serve from both classpath (for existing static images)
     * and external uploads directory (for user-uploaded images)
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve uploaded images from external uploads directory (checked first)
        // This directory is outside the classpath to avoid issues with Spring Boot DevTools
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:uploads/images/")
                .addResourceLocations("classpath:/static/images/");
        
        // Serve uploaded documents (ID cards, driver licenses, etc.)
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/")
                .addResourceLocations("classpath:/static/uploads/");
    }

    /**
     * Get the upload directory path
     */
    public String getUploadDir() {
        return UPLOAD_DIR;
    }
}
