package com.example.handPick.security;

import com.example.handPick.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList; // For empty list of authorities

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    // Constructor injection
    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        String username = null; // Changed to String as it might be null

        // 1. Check for Authorization header and "Bearer " prefix
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extract JWT token
        jwt = authHeader.substring(7); // "Bearer " is 7 characters long

        try {
            username = jwtService.extractUsername(jwt); // Extract username (subject) from token
        } catch (Exception e) {
            // Log the error (e.g., token expired, invalid signature)
            // System.out.println("JWT processing error: " + e.getMessage());
            filterChain.doFilter(request, response); // Continue filter chain, but don't authenticate
            return;
        }


        // 3. Authenticate if username (subject) is found and no existing authentication in context
        // This check ensures we don't re-authenticate an already authenticated request
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Validate the token (only checks expiration and signature now)
            if (jwtService.isTokenValid(jwt)) { // Simplified: removed UserDetails parameter
                // Create a Spring Security Authentication object.
                // For a "plain" JWT, we might not have rich UserDetails from a DB.
                // We'll use the username from the token as the principal.
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                username,           // Principal (the username from the token)
                                null,               // Credentials (null for JWT after validation)
                                new ArrayList<>()   // Authorities (empty list if no roles in token/DB)
                        );

                // Set authentication details (like remote IP address)
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Set the authentication in the SecurityContextHolder
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 4. Continue the filter chain
        filterChain.doFilter(request, response);
    }
}