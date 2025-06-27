package com.example.handPick.config;

import com.example.handPick.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Enable @PreAuthorize and @PostAuthorize for method-level security
public class SecurityConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Value("${allowed.origins}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return authenticationProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Split allowedOrigins string by comma to support multiple origins
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Cookie"));
        configuration.setAllowCredentials(true); // Crucial for sending/receiving cookies across origins
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Set-Cookie")); // Expose these headers to the client

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Apply CORS to all paths
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Disable CSRF for stateless API (JWT)
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Enable CORS with custom configuration
                .authorizeHttpRequests(authorize -> authorize
                        // Public endpoints:
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/auth/logout",
                                "/api/cart/**", // Cart operations are public for guests and authenticated users
                                "/api/products", // GET /api/products should be public for product listing
                                "/api/products/{id}", // GET /api/products/{id} should be public for single product view
                                "/h2-console/**" // H2 console for development
                        ).permitAll() // Permit all access to the above paths
                        // All other requests require authentication.
                        // POST, PUT, DELETE for /api/products are secured by @PreAuthorize on the controller methods themselves.
                        .anyRequest().authenticated() // Any other request not explicitly permitted above requires authentication
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Use stateless sessions for JWT
                )
                .authenticationProvider(authenticationProvider()) // Configure custom authentication provider
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class) // Add JWT filter before Spring Security's default filter
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin())); // Allow H2 console to be in an iframe

        return http.build();
    }
}