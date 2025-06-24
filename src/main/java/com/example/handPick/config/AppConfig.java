package com.example.handPick.config; // Adjusted package name

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AppConfig implements WebMvcConfigurer {

    @Value("${allowed.origins}")
    private String[] allowedOrigins;

    // Use @Value("${spring.data.rest.base-path:/api}") to provide a default if not set
    @Value("${spring.data.rest.base-path:/api}")
    private String basePath;

    @Override
    public void addCorsMappings(CorsRegistry cors) {

        // Set up CORS mapping for all endpoints under the base path
        cors.addMapping(basePath + "/**") // Applies CORS to all paths under /api (or your configured base-path)
                .allowedOrigins(allowedOrigins) // Specify allowed origins from properties
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS") // Common HTTP methods for a REST API
                .allowedHeaders("*") // Allow all common headers (e.g., Content-Type, Authorization, Accept)
                .allowCredentials(true) // Important if you're sending cookies, authorization headers, or session data
                .maxAge(3600); // How long the pre-flight request results can be cached (in seconds)
    }
}