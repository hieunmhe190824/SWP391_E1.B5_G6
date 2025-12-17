package com.carrental.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Keep a simple handler for explicit /static/** paths if used anywhere
        // Note: default Spring Boot mapping already serves resources from classpath:/static/**
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");

        // Do NOT re-map "/uploads/**" here.
        // FileUploadConfig already configures "/uploads/**" to serve from the external
        // "uploads" directory as well as classpath:/static/uploads/.
        // Duplicating the handler here would override that configuration and break
        // serving user-uploaded documents and images.
    }

}
