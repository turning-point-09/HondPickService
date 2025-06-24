package com.example.handPick.config; // UPDATED

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.example.handPick.service.CustomUserDetailsService; // UPDATED

import java.io.IOException;
import java.util.UUID;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;
        UUID guestId = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            try {
                // Try to extract username first (for logged-in users)
                username = jwtUtil.extractUsername(token);
            } catch (Exception e) {
                // If username extraction fails, it might be a guest token or an invalid token
                try {
                    guestId = jwtUtil.extractGuestId(token);
                } catch (Exception ex) {
                    // Log token parsing failures but continue.
                    // This can happen for expired tokens, malformed tokens, or if it's not a guest/user token.
                    System.err.println("JWT token parsing failed (not a valid user or guest token): " + ex.getMessage());
                }
            }
        }

        // --- Handle Logged-in User (Access Token) ---
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (jwtUtil.validateToken(token, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        // --- Handle Guest User (Guest Token) ---
        // If no authenticated user, but a guest ID was extracted, set it as a request attribute.
        // This allows the CartController to access the guestId without a full Spring Security authentication.
        else if (guestId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Note: We don't create a SecurityContext for anonymous users with guestId here.
            // The guestId is simply passed along as a request attribute for specific services like CartService.
            request.setAttribute("guestId", guestId);
        }

        filterChain.doFilter(request, response);
    }
}