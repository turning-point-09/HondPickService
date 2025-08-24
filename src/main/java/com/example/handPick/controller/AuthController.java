package com.example.handPick.controller;

import com.example.handPick.config.JwtUtil;
import com.example.handPick.dto.AuthResponse;
import com.example.handPick.dto.LoginRequest;
import com.example.handPick.dto.RegisterRequest;
import com.example.handPick.dto.ForgotPasswordRequest;
import com.example.handPick.dto.ResetPasswordRequest;
import com.example.handPick.model.RefreshToken;
import com.example.handPick.model.User;
import com.example.handPick.service.CartService;
import com.example.handPick.service.RefreshTokenService;
import com.example.handPick.service.UserService;
import com.example.handPick.service.PasswordResetService;
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
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    @Value("${app.cookies.secure:false}")
    private boolean cookiesSecure;

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

    @Autowired
    private PasswordResetService passwordResetService;

    /**
     * Helper method to set JWT and Refresh Token cookies in the HTTP response.
     * Sets Secure flag for production. Optionally set SameSite if supported.
     */
    private void setAuthCookies(HttpServletResponse response, String accessToken, String refreshToken, long accessTokenMaxAgeMs, int refreshTokenMaxAgeSeconds) {
        Cookie accessTokenCookie = new Cookie("jwt_token", accessToken);
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge((int) (accessTokenMaxAgeMs / 1000));
        accessTokenCookie.setSecure(cookiesSecure); // Use configuration property
        // Optionally set SameSite if supported by your Servlet container
        // accessTokenCookie.setAttribute("SameSite", "Strict");
        response.addCookie(accessTokenCookie);

        Cookie refreshTokenCookie = new Cookie("refresh_token", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setPath("/api/auth/refresh");
        refreshTokenCookie.setMaxAge(refreshTokenMaxAgeSeconds);
        refreshTokenCookie.setSecure(cookiesSecure); // Use configuration property
        // refreshTokenCookie.setAttribute("SameSite", "Strict");
        response.addCookie(refreshTokenCookie);

        logger.debug("Auth cookies set. Access token max age: {}s, Refresh token max age: {}s",
                (accessTokenMaxAgeMs / 1000), refreshTokenMaxAgeSeconds);
    }

    /**
     * Utility to extract a cookie by name from the request.
     */
    private Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return Optional.empty();
        return Arrays.stream(request.getCookies())
                .filter(cookie -> name.equals(cookie.getName()))
                .findFirst();
    }

    /**
     * Handles user registration. Creates a new user with provided details and issues access and refresh tokens.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletResponse httpResponse) {
        try {
            User newUser = userService.registerUser(
                    request.getMobileNumber(),
                    request.getPassword(),
                    request.getEmail(),
                    request.getFirstName(),
                    request.getLastName(),
                    request.getStreet(),
                    request.getCity(),
                    request.getState(),
                    request.getPostalCode(),
                    request.getCountry()
            );
            String accessToken = jwtUtil.generateAccessToken(newUser.getMobileNumber(), newUser.getId());
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(newUser.getId());

            setAuthCookies(httpResponse, accessToken, refreshToken.getToken(), jwtUtil.getExpiration(), (int)(jwtUtil.getRefreshExpiration() / 1000));

            logger.info("User registered successfully: {}", newUser.getMobileNumber());
            return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(null, "User registered successfully!"));
        } catch (RuntimeException e) {
            logger.error("User registration failed for {}: {}", request.getMobileNumber(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new AuthResponse(null, e.getMessage()));
        }
    }

    /**
     * Handles user login. Authenticates user and issues tokens.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse httpResponse) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getMobileNumber(), request.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User loggedInUser = userService.findByMobileNumber(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found after authentication."));

            String accessToken = jwtUtil.generateAccessToken(userDetails.getUsername(), loggedInUser.getId());
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(loggedInUser.getId());

            setAuthCookies(httpResponse, accessToken, refreshToken.getToken(), jwtUtil.getExpiration(), (int)(jwtUtil.getRefreshExpiration() / 1000));

            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Bearer " + accessToken);

            logger.info("User {} logged in successfully.", loggedInUser.getMobileNumber());
            return ResponseEntity.ok().headers(headers).body(new AuthResponse(null, "Login successful!"));

        } catch (Exception e) {
            logger.error("Authentication failed for user {}: {}", request.getMobileNumber(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponse(null, "Authentication failed: " + e.getMessage()));
        }
    }

    /**
     * Handles token refresh. Uses a refresh token from cookie to issue a new access token.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Optional<Cookie> refreshCookie = getCookie(httpRequest, "refresh_token");
        String requestRefreshToken = refreshCookie.map(Cookie::getValue).orElse(null);

        if (requestRefreshToken != null && !requestRefreshToken.isEmpty()) {
            try {
                RefreshToken storedRefreshToken = refreshTokenService.findByToken(requestRefreshToken)
                        .orElseThrow(() -> new RuntimeException("Refresh token not found in database!"));

                refreshTokenService.verifyExpiration(storedRefreshToken);

                String mobileNumber = storedRefreshToken.getUser().getMobileNumber();
                Long userId = storedRefreshToken.getUser().getId();
                String newAccessToken = jwtUtil.generateAccessToken(mobileNumber, userId);

                setAuthCookies(httpResponse, newAccessToken, storedRefreshToken.getToken(), jwtUtil.getExpiration(), (int)(jwtUtil.getRefreshExpiration() / 1000));

                HttpHeaders headers = new HttpHeaders();
                headers.add("Authorization", "Bearer " + newAccessToken);
                logger.info("Access token refreshed successfully for user: {}", mobileNumber);
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
     */
    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout(@AuthenticationPrincipal UserDetails userDetails, HttpServletResponse httpResponse) {
        if (userDetails != null) {
            User currentUser = userService.findByMobileNumber(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found for logout."));
            refreshTokenService.deleteByUserId(currentUser.getId());
            SecurityContextHolder.clearContext();
            logger.info("User {} logged out successfully. Refresh token deleted.", currentUser.getMobileNumber());
        } else {
            logger.info("Logout request from unauthenticated user.");
        }

        Cookie accessTokenCookie = new Cookie("jwt_token", "");
        accessTokenCookie.setPath("/");
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setMaxAge(0);
        accessTokenCookie.setSecure(cookiesSecure);
        // accessTokenCookie.setAttribute("SameSite", "Strict");
        httpResponse.addCookie(accessTokenCookie);

        Cookie refreshTokenCookie = new Cookie("refresh_token", "");
        refreshTokenCookie.setPath("/api/auth/refresh");
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setMaxAge(0);
        refreshTokenCookie.setSecure(cookiesSecure);
        // refreshTokenCookie.setAttribute("SameSite", "Strict");
        httpResponse.addCookie(refreshTokenCookie);

        return ResponseEntity.ok(new AuthResponse(null, "Logged out successfully!"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        passwordResetService.createAndSendResetToken(request.getEmail());
        return ResponseEntity.ok("Password reset link sent to your email (check logs in dev mode).");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok("Password has been reset successfully.");
    }
}