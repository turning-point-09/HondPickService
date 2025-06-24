package com.example.handPick.controller;

import com.example.handPick.config.JwtUtil;
import com.example.handPick.dto.AddToCartRequest;
import com.example.handPick.dto.CartDto;
import com.example.handPick.dto.CheckoutResponse; // NEW IMPORT
import com.example.handPick.model.Order; // NEW IMPORT
import com.example.handPick.model.User;
import com.example.handPick.service.CartService;
import com.example.handPick.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // NEW IMPORT for @PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.Cookie;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    // ... (existing getCart and addItemToCart methods) ...

    /**
     * Endpoint to get the current user's or guest's cart.
     * If user is logged in, their cart is returned.
     * If not logged in, tries to retrieve cart based on guest_token cookie.
     * If no guest_token, a new one is generated and set.
     * @param userDetails Authenticated user details (if available from access token).
     * @param httpRequest HttpServletRequest to read guest_token cookie.
     * @param httpResponse HttpServletResponse to set new guest_token cookie if needed.
     * @return ResponseEntity with CartDto.
     */
    @GetMapping
    public ResponseEntity<CartDto> getCart(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        User currentUser = null;
        // If userDetails is present, it means a valid access token was provided in the Authorization header.
        if (userDetails != null) {
            currentUser = userService.findByUsername(userDetails.getUsername()).orElse(null);
        }

        UUID guestId = null;
        // Always try to get guest ID from cookie, regardless of login status.
        // This is crucial for initial guest experience and potential cart merging on login.
        Optional<Cookie> guestCookie = Arrays.stream(Optional.ofNullable(httpRequest.getCookies()).orElse(new Cookie[0]))
                .filter(cookie -> "guest_token".equals(cookie.getName()))
                .findFirst();

        if (guestCookie.isPresent()) {
            String guestJwt = guestCookie.get().getValue();
            try {
                guestId = jwtUtil.extractGuestId(guestJwt);
                // Invalidate guestId if the guest token is expired or invalid
                if (!jwtUtil.validateGuestToken(guestJwt)) {
                    guestId = null;
                    // Clear the expired guest cookie
                    Cookie expiredGuestCookie = new Cookie("guest_token", "");
                    expiredGuestCookie.setPath("/");
                    expiredGuestCookie.setHttpOnly(true);
                    expiredGuestCookie.setMaxAge(0);
                    httpResponse.addCookie(expiredGuestCookie);
                }
            } catch (Exception e) {
                System.err.println("Failed to extract guest ID from cookie: " + e.getMessage());
                guestId = null;
            }
        }

        // If no logged-in user and no valid guest ID, create a new guest ID and token.
        if (currentUser == null && guestId == null) {
            guestId = UUID.randomUUID();
            String newGuestToken = jwtUtil.generateGuestToken(guestId);

            Cookie newGuestCookie = new Cookie("guest_token", newGuestToken);
            newGuestCookie.setHttpOnly(true);
            newGuestCookie.setPath("/");
            newGuestCookie.setMaxAge((int) (jwtUtil.getGuestExpiration() / 1000)); // Max age in seconds
            httpResponse.addCookie(newGuestCookie);
        }

        // Retrieve or create the cart based on user or guest ID
        CartDto cart = cartService.getCart(currentUser, guestId);
        return ResponseEntity.ok(cart);
    }

    /**
     * Endpoint to add items to the cart.
     * Supports both authenticated users and anonymous guests.
     * @param request AddToCartRequest containing product details and quantity.
     * @param userDetails Authenticated user details (if available from access token).
     * @param httpRequest HttpServletRequest to read guest_token cookie.
     * @param httpResponse HttpServletResponse to set new guest_token cookie if needed.
     * @return ResponseEntity with updated CartDto or error.
     */
    @PostMapping("/add")
    public ResponseEntity<CartDto> addItemToCart(
            @RequestBody AddToCartRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        User currentUser = null;
        if (userDetails != null) {
            currentUser = userService.findByUsername(userDetails.getUsername()).orElse(null);
        }

        UUID guestId = null;
        // Try to get guest ID from cookie
        Optional<Cookie> guestCookie = Arrays.stream(Optional.ofNullable(httpRequest.getCookies()).orElse(new Cookie[0]))
                .filter(cookie -> "guest_token".equals(cookie.getName()))
                .findFirst();

        if (guestCookie.isPresent()) {
            String guestJwt = guestCookie.get().getValue();
            try {
                guestId = jwtUtil.extractGuestId(guestJwt);
                if (!jwtUtil.validateGuestToken(guestJwt)) {
                    guestId = null;
                    Cookie expiredGuestCookie = new Cookie("guest_token", "");
                    expiredGuestCookie.setPath("/");
                    expiredGuestCookie.setHttpOnly(true);
                    expiredGuestCookie.setMaxAge(0);
                    httpResponse.addCookie(expiredGuestCookie);
                }
            } catch (Exception e) {
                System.err.println("Failed to extract guest ID from cookie during add to cart: " + e.getMessage());
                guestId = null;
            }
        }

        // If no logged-in user and no valid guest ID, create a new guest ID and token.
        if (currentUser == null && guestId == null) {
            guestId = UUID.randomUUID();
            String newGuestToken = jwtUtil.generateGuestToken(guestId);

            Cookie newGuestCookie = new Cookie("guest_token", newGuestToken);
            newGuestCookie.setHttpOnly(true);
            newGuestCookie.setPath("/");
            newGuestCookie.setMaxAge((int) (jwtUtil.getGuestExpiration() / 1000));
            httpResponse.addCookie(newGuestCookie);
        }

        try {
            CartDto updatedCart = cartService.addItemToCart(currentUser, guestId, request);
            return ResponseEntity.ok(updatedCart);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null); // Or a custom error DTO
        }
    }


    /**
     * Endpoint to finalize the cart and create an order.
     * Requires an authenticated user.
     * @param userDetails Authenticated user details.
     * @return ResponseEntity with Order ID or error message.
     */
    @PreAuthorize("isAuthenticated()") // Ensure only logged-in users can checkout
    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkoutCart(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new CheckoutResponse(null, "User not authenticated for checkout.", false));
        }

        try {
            User currentUser = userService.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found during checkout."));

            Order newOrder = cartService.checkoutCart(currentUser);

            return ResponseEntity.ok(new CheckoutResponse(newOrder.getId(), "Order placed successfully!", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new CheckoutResponse(null, e.getMessage(), false));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new CheckoutResponse(null, "Failed to place order: " + e.getMessage(), false));
        }
    }
}