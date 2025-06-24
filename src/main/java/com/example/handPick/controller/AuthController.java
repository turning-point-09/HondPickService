package com.example.handPick.controller; // Ensure this is correct

import com.example.handPick.config.JwtUtil;
import com.example.handPick.dto.AuthResponse;
import com.example.handPick.dto.LoginRequest;
import com.example.handPick.model.RefreshToken; // Ensure this import is correct
import com.example.handPick.model.User;
import com.example.handPick.service.CartService;
import com.example.handPick.service.RefreshTokenService;
import com.example.handPick.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

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

    private void setAuthCookies(HttpServletResponse response, String accessToken, String refreshToken, long accessTokenMaxAgeMs, int refreshTokenMaxAgeSeconds) {
        Cookie accessTokenCookie = new Cookie("jwt_token", accessToken);
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge((int) (accessTokenMaxAgeMs / 1000));
        // accessTokenCookie.setSecure(true); // Uncomment for HTTPS in production
        response.addCookie(accessTokenCookie);

        Cookie refreshTokenCookie = new Cookie("refresh_token", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setPath("/api/auth/refresh");
        refreshTokenCookie.setMaxAge(refreshTokenMaxAgeSeconds);
        // refreshTokenCookie.setSecure(true); // Uncomment for HTTPS in production
        response.addCookie(refreshTokenCookie);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody LoginRequest request, HttpServletResponse httpResponse) {
        try {
            User newUser = userService.registerUser(request.getUsername(), request.getPassword(), "default@example.com");
            String accessToken = jwtUtil.generateAccessToken(newUser.getUsername(), newUser.getId());
            // Ensure RefreshToken is found and used correctly here
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(newUser.getId());

            setAuthCookies(httpResponse, accessToken, refreshToken.getToken(), jwtUtil.getExpiration(), (int)(refreshToken.getExpiryDate().toEpochMilli() - Instant.now().toEpochMilli())/1000);

            return ResponseEntity.ok(new AuthResponse(accessToken, "User registered successfully!"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new AuthResponse(null, e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User loggedInUser = userService.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found after authentication."));

            String accessToken = jwtUtil.generateAccessToken(userDetails.getUsername(), loggedInUser.getId());
            // Ensure RefreshToken is found and used correctly here
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(loggedInUser.getId());

            UUID guestId = null;
            Optional<Cookie> guestCookie = Arrays.stream(Optional.ofNullable(httpRequest.getCookies()).orElse(new Cookie[0]))
                    .filter(cookie -> "guest_token".equals(cookie.getName()))
                    .findFirst();

            if (guestCookie.isPresent()) {
                String guestJwt = guestCookie.get().getValue();
                try {
                    guestId = jwtUtil.extractGuestId(guestJwt);
                    if (!jwtUtil.validateGuestToken(guestJwt)) {
                        guestId = null;
                    }
                } catch (Exception e) {
                    System.err.println("Failed to extract guest ID from cookie during login: " + e.getMessage());
                    guestId = null;
                }
            }

            if (guestId != null) {
                cartService.mergeCarts(loggedInUser, guestId);
                Cookie oldGuestCookie = new Cookie("guest_token", "");
                oldGuestCookie.setPath("/");
                oldGuestCookie.setHttpOnly(true);
                oldGuestCookie.setMaxAge(0);
                httpResponse.addCookie(oldGuestCookie);
            }

            setAuthCookies(httpResponse, accessToken, refreshToken.getToken(), jwtUtil.getExpiration(), (int)(refreshToken.getExpiryDate().toEpochMilli() - Instant.now().toEpochMilli())/1000);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Bearer " + accessToken);

            return ResponseEntity.ok().headers(headers).body(new AuthResponse(accessToken, "Login successful!"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponse(null, "Authentication failed: " + e.getMessage()));
        }
    }

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
                // Ensure RefreshToken is found and used correctly here
                RefreshToken storedRefreshToken = refreshTokenService.findByToken(requestRefreshToken)
                        .orElseThrow(() -> new RuntimeException("Refresh token not found in database!"));

                refreshTokenService.verifyExpiration(storedRefreshToken);

                String username = storedRefreshToken.getUser().getUsername();
                Long userId = storedRefreshToken.getUser().getId();
                String newAccessToken = jwtUtil.generateAccessToken(username, userId);

                setAuthCookies(httpResponse, newAccessToken, storedRefreshToken.getToken(), jwtUtil.getExpiration(), (int)(storedRefreshToken.getExpiryDate().toEpochMilli() - Instant.now().toEpochMilli())/1000);

                HttpHeaders headers = new HttpHeaders();
                headers.add("Authorization", "Bearer " + newAccessToken);
                return ResponseEntity.ok().headers(headers).body(new AuthResponse(newAccessToken, "Access token refreshed successfully!"));

            } catch (RuntimeException e) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new AuthResponse(null, e.getMessage()));
            }
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new AuthResponse(null, "Refresh token is missing from cookies!"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout(@AuthenticationPrincipal UserDetails userDetails, HttpServletResponse httpResponse) {
        if (userDetails != null) {
            User currentUser = userService.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found for logout."));
            refreshTokenService.deleteByUserId(currentUser.getId());
            SecurityContextHolder.clearContext();
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