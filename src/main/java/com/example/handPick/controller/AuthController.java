package com.example.handPick.controller;

import com.example.handPick.config.JwtUtil;
import com.example.handPick.dto.AuthResponse;
import com.example.handPick.dto.LoginRequest;
import com.example.handPick.dto.RegisterRequest;
import com.example.handPick.model.RefreshToken;
import com.example.handPick.model.User;
import com.example.handPick.service.CartService;
import com.example.handPick.service.RefreshTokenService;
import com.example.handPick.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @Autowired
    private CartService cartService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    /**
     * Helper method to set JWT and Refresh Token cookies in the HTTP response.
     * @param response HttpServletResponse to add cookies to.
     * @param accessToken The generated access token string.
     * @param refreshToken The generated refresh token string.
     * @param accessTokenMaxAgeMs Max age for access token cookie in milliseconds.
     * @param refreshTokenMaxAgeSeconds Max age for refresh token cookie in seconds.
     */
    private void setAuthCookies(HttpServletResponse response, String accessToken, String refreshToken, long accessTokenMaxAgeMs, int refreshTokenMaxAgeSeconds) {
        Cookie accessTokenCookie = new Cookie("jwt_token", accessToken);
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge((int) (accessTokenMaxAgeMs / 1000));
        response.addCookie(accessTokenCookie);

        Cookie refreshTokenCookie = new Cookie("refresh_token", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setPath("/api/auth/refresh");
        refreshTokenCookie.setMaxAge(refreshTokenMaxAgeSeconds);
        response.addCookie(refreshTokenCookie);

        logger.debug("Auth cookies set. Access token max age: {}s, Refresh token max age: {}s",
                (accessTokenMaxAgeMs / 1000), refreshTokenMaxAgeSeconds);
    }

    /**
     * Handles user registration. Creates a new user with provided details and issues access and refresh tokens.
     * @param request RegisterRequest containing username, password, email, and optional address/personal details.
     * @param httpResponse HttpServletResponse to set cookies.
     * @return ResponseEntity with AuthResponse indicating success or failure.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletResponse httpResponse) {
        try {
            User newUser = userService.registerUser(
                    request.getUsername(),
                    request.getPassword(),
                    request.getEmail(),
                    request.getFirstName(),    // Pass firstName
                    request.getLastName(),     // Pass lastName
                    request.getMobileNumber(), // Pass mobileNumber
                    request.getStreet(),       // Pass street
                    request.getCity(),         // Pass city
                    request.getState(),        // Pass state
                    request.getPostalCode(),   // Pass postalCode
                    request.getCountry()       // Pass country
            );
            String accessToken = jwtUtil.generateAccessToken(newUser.getUsername(), newUser.getId());
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(newUser.getId());

            setAuthCookies(httpResponse, accessToken, refreshToken.getToken(), jwtUtil.getExpiration(), (int)(jwtUtil.getRefreshExpiration() / 1000));

            logger.info("User registered successfully: {}", newUser.getUsername());
            return ResponseEntity.ok(new AuthResponse(accessToken, "User registered successfully!"));
        } catch (RuntimeException e) {
            logger.error("User registration failed for {}: {}", request.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new AuthResponse(null, e.getMessage()));
        }
    }

    /**
     * Handles user login. Authenticates user and issues tokens.
     * @param request LoginRequest containing username and password.
     * @param httpResponse HttpServletResponse to set auth cookies.
     * @return ResponseEntity with AuthResponse indicating success or failure.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request, HttpServletResponse httpResponse) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User loggedInUser = userService.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found after authentication."));

            String accessToken = jwtUtil.generateAccessToken(userDetails.getUsername(), loggedInUser.getId());
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(loggedInUser.getId());

            setAuthCookies(httpResponse, accessToken, refreshToken.getToken(), jwtUtil.getExpiration(), (int)(jwtUtil.getRefreshExpiration() / 1000));

            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Bearer " + accessToken);

            logger.info("User {} logged in successfully.", loggedInUser.getUsername());
            return ResponseEntity.ok().headers(headers).body(new AuthResponse(accessToken, "Login successful!"));

        } catch (Exception e) {
            logger.error("Authentication failed for user {}: {}", request.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponse(null, "Authentication failed: " + e.getMessage()));
        }
    }

    /**
     * Handles token refresh. Uses a refresh token from cookie to issue a new access token.
     * @param httpRequest HttpServletRequest to get refresh token cookie.
     * @param httpResponse HttpServletResponse to set new access token cookie.
     * @return ResponseEntity with AuthResponse indicating success or failure.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String requestRefreshToken = null;
        Optional<Cookie> refreshCookie = Arrays.stream(Optional.ofNullable(httpRequest.getCookies()).orElse(new Cookie[0]))
                .filter(cookie -> "refresh_token".equals(cookie.getName()))
                .findFirst();

        if (refreshCookie.isPresent()) {
            requestRefreshToken = refreshCookie.get().getValue();
        }

        if (requestRefreshToken != null && !requestRefreshToken.isEmpty()) {
            try {
                RefreshToken storedRefreshToken = refreshTokenService.findByToken(requestRefreshToken)
                        .orElseThrow(() -> new RuntimeException("Refresh token not found in database!"));

                refreshTokenService.verifyExpiration(storedRefreshToken);

                String username = storedRefreshToken.getUser().getUsername();
                Long userId = storedRefreshToken.getUser().getId();
                String newAccessToken = jwtUtil.generateAccessToken(username, userId);

                setAuthCookies(httpResponse, newAccessToken, storedRefreshToken.getToken(), jwtUtil.getExpiration(), (int)(jwtUtil.getRefreshExpiration() / 1000));

                HttpHeaders headers = new HttpHeaders();
                headers.add("Authorization", "Bearer " + newAccessToken);
                logger.info("Access token refreshed successfully for user: {}", username);
                return ResponseEntity.ok().headers(headers).body(new AuthResponse(newAccessToken, "Access token refreshed successfully!"));

            } catch (RuntimeException e) {
                logger.error("Refresh token failed: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new AuthResponse(null, e.getMessage()));
            }
        } else {
            logger.warn("Refresh token is missing from cookies during refresh request.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new AuthResponse(null, "Refresh token is missing from cookies!"));
        }
    }

    /**
     * Handles user logout. Deletes refresh token and clears authentication context and cookies.
     * @param userDetails Authenticated user details (injected by Spring Security).
     * @param httpResponse HttpServletResponse to clear cookies.
     * @return ResponseEntity with AuthResponse indicating success.
     */
    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout(@AuthenticationPrincipal UserDetails userDetails, HttpServletResponse httpResponse) {
        if (userDetails != null) {
            User currentUser = userService.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found for logout."));
            refreshTokenService.deleteByUserId(currentUser.getId());
            SecurityContextHolder.clearContext();
            logger.info("User {} logged out successfully. Refresh token deleted.", currentUser.getUsername());
        } else {
            logger.info("Logout request from unauthenticated user.");
        }

        Cookie accessTokenCookie = new Cookie("jwt_token", "");
        accessTokenCookie.setPath("/");
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setMaxAge(0);
        httpResponse.addCookie(accessTokenCookie);

        Cookie refreshTokenCookie = new Cookie("refresh_token", "");
        refreshTokenCookie.setPath("/api/auth/refresh");
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setMaxAge(0);
        httpResponse.addCookie(refreshTokenCookie);

        return ResponseEntity.ok(new AuthResponse(null, "Logged out successfully!"));
    }
}