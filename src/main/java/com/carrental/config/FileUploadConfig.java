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
    
    /**
     * Configure static resource handlers
     * Map /images/** to the static/images directory
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve uploaded images from static/images
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
    }
}
