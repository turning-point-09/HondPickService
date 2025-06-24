package com.example.handPick.config;

import com.example.handPick.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;

@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    // Constructor injection for JwtAuthFilter
    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Disable CSRF for stateless REST APIs
                .cors(cors -> {}) // Enable CORS (assumes CorsConfigurationSource is defined elsewhere, like AppConfig)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/orders/**").authenticated() // Example: secure specific endpoints
                        .anyRequest().permitAll()) // Permit all other requests by default (adjust as needed)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Use stateless sessions for JWT
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class) // Add custom JWT filter
                .setSharedObject(ContentNegotiationStrategy.class, new HeaderContentNegotiationStrategy()); // Configure content negotiation

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Expose AuthenticationManager for potential manual username/password authentication flow
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}