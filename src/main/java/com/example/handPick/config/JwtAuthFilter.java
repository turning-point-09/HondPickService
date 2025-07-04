package com.example.handPick.config;

import com.example.handPick.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String jwtToken = null;
        String username = null;

        // 1. Try to extract JWT from "jwt_token" cookie
        Optional<Cookie> jwtCookie = Arrays.stream(Optional.ofNullable(request.getCookies()).orElse(new Cookie[0]))
                .filter(cookie -> "jwt_token".equals(cookie.getName()))
                .findFirst();

        if (jwtCookie.isPresent()) {
            jwtToken = jwtCookie.get().getValue();
            try {
                username = jwtUtil.extractUsername(jwtToken);
                logger.debug("JWT Token found in cookie. Extracted username: {}", username);
            } catch (ExpiredJwtException e) {
                logger.warn("JWT Token is expired: {}", e.getMessage());
                // Optionally clear the expired cookie
                clearCookie(response, "jwt_token", "/");
            } catch (SignatureException e) {
                logger.warn("JWT Token has invalid signature: {}", e.getMessage());
                clearCookie(response, "jwt_token", "/");
            } catch (MalformedJwtException e) {
                logger.warn("JWT Token is malformed: {}", e.getMessage());
                clearCookie(response, "jwt_token", "/");
            } catch (IllegalArgumentException e) {
                logger.warn("Unable to get JWT Token or JWT claims string is empty: {}", e.getMessage());
            } catch (Exception e) {
                logger.error("An unexpected error occurred during JWT token processing: {}", e.getMessage(), e);
            }
        } else {
            logger.debug("No 'jwt_token' cookie found in request.");
        }

        // 2. If username is extracted and no authentication is currently set in SecurityContext
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = null;
            try {
                userDetails = userDetailsService.loadUserByUsername(username);
            } catch (Exception e) {
                logger.warn("User details not found for username {}: {}", username, e.getMessage());
                // If user details cannot be loaded, it means the user might no longer exist or there's a DB issue.
                // Invalidate the token to force re-authentication.
                clearCookie(response, "jwt_token", "/");
                username = null; // Prevent further processing with this username
            }

            if (userDetails != null && jwtToken != null && jwtUtil.validateToken(jwtToken, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                logger.debug("User {} authenticated successfully via JWT.", username);
            } else if (jwtToken != null) {
                logger.warn("JWT Token validation failed for user: {}", username);
                clearCookie(response, "jwt_token", "/");
            }
        }

        // 3. Continue the filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Helper method to clear a specific cookie.
     * @param response HttpServletResponse to add the expired cookie to.
     * @param name The name of the cookie to clear.
     * @param path The path of the cookie to clear.
     */
    private void clearCookie(HttpServletResponse response, String name, String path) {
        Cookie cookie = new Cookie(name, "");
        cookie.setPath(path);
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0); // Set max age to 0 to delete the cookie
        response.addCookie(cookie);
        logger.debug("Cleared cookie: {}", name);
    }
}
