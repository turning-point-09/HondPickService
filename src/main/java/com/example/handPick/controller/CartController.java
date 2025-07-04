package com.example.handPick.controller;

import com.example.handPick.config.JwtUtil;
import com.example.handPick.dto.AddToCartRequest;
import com.example.handPick.dto.CartDto;
import com.example.handPick.dto.CheckoutRequest;
import com.example.handPick.dto.CheckoutResponse;
import com.example.handPick.dto.QuantityUpdateRequest;
import com.example.handPick.model.Order;
import com.example.handPick.model.User;
import com.example.handPick.service.CartService;
import com.example.handPick.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders; // Import HttpHeaders for setting cookies properly
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie; // Import ResponseCookie for better cookie management
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private static final Logger logger = LoggerFactory.getLogger(CartController.class);

    private final CartService cartService;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Autowired
    public CartController(CartService cartService,
                          UserService userService,
                          JwtUtil jwtUtil) {
        this.cartService = cartService;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Retrieves the current user's shopping cart.
     * GET /api/cart
     * @param userDetails Authenticated user details (if logged in).
     * @return ResponseEntity with CartDto.
     */
    @GetMapping
    public ResponseEntity<CartDto> getCart(@AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.findByUsername(userDetails.getUsername()).orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        CartDto cart = cartService.getCart(currentUser, null);
        return ResponseEntity.ok(cart);
    }

    /**
     * Adds an item to the current user's cart.
     * POST /api/cart/add
     * @param reqDto AddToCartRequest containing productId and quantity.
     * @param userDetails Authenticated user details (if logged in).
     * @return ResponseEntity with updated CartDto.
     */
    @PostMapping("/add")
    public ResponseEntity<CartDto> addItemToCart(
            @Valid @RequestBody AddToCartRequest reqDto, // @Valid for DTO validation
            @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = userService.findByUsername(userDetails.getUsername()).orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            CartDto updated = cartService.addItemToCart(currentUser, reqDto);
            logger.info("Item added to cart. Product ID: {}, Quantity: {}", reqDto.getProductId(), reqDto.getQuantity());
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException ex) {
            logger.error("Failed to add item to cart: {}", ex.getMessage());
            // Return a CartDto with error message for frontend consumption
            CartDto error = new CartDto(
                    null, Collections.emptyList(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, null, ex.getMessage()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception ex) {
            logger.error("Server error while adding item to cart: {}", ex.getMessage(), ex);
            CartDto error = new CartDto(
                    null, Collections.emptyList(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, null, "Server error: " + ex.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Updates the quantity of an item in the current user's cart.
     * PUT /api/cart/item/{productId}
     * @param productId The ID of the product to update.
     * @param reqDto QuantityUpdateRequest containing the new quantity.
     * @param userDetails Authenticated user details (if logged in).
     * @return ResponseEntity with updated CartDto.
     */
    @PutMapping("/item/{productId}")
    public ResponseEntity<CartDto> updateItemQuantity(
            @PathVariable Long productId,
            @Valid @RequestBody QuantityUpdateRequest reqDto, // @Valid for DTO validation
            @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = userService.findByUsername(userDetails.getUsername()).orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            CartDto updated = cartService.updateItemQuantity(
                    currentUser, null, productId, reqDto.getQuantity());
            logger.info("Cart item quantity updated for Product ID: {} to Quantity: {}", productId, reqDto.getQuantity());
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException ex) {
            logger.error("Failed to update item quantity in cart: {}", ex.getMessage());
            CartDto error = new CartDto(
                    null, Collections.emptyList(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, null, ex.getMessage()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception ex) {
            logger.error("Server error while updating item quantity in cart: {}", ex.getMessage(), ex);
            CartDto error = new CartDto(
                    null, Collections.emptyList(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, null, "Server error: " + ex.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Removes an item from the current user's cart.
     * DELETE /api/cart/item/{productId}
     * @param productId The ID of the product to remove.
     * @param userDetails Authenticated user details (if logged in).
     * @return ResponseEntity with updated CartDto.
     */
    @DeleteMapping("/item/{productId}")
    public ResponseEntity<CartDto> removeItem(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserDetails userDetails) {

        User currentUser = userService.findByUsername(userDetails.getUsername()).orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            CartDto updated = cartService.removeItemFromCart(
                    currentUser, null, productId);
            logger.info("Item removed from cart. Product ID: {}", productId);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException ex) {
            logger.error("Failed to remove item from cart: {}", ex.getMessage());
            CartDto error = new CartDto(
                    null, Collections.emptyList(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, null, ex.getMessage()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception ex) {
            logger.error("Server error while removing item from cart: {}", ex.getMessage(), ex);
            CartDto error = new CartDto(
                    null, Collections.emptyList(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, null, "Server error: " + ex.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Handles the checkout process for an authenticated user.
     * Requires authentication.
     * POST /api/cart/checkout
     * @param userDetails Authenticated user details.
     * @param checkoutRequest CheckoutRequest containing the chosen payment method and optional shipping address.
     * @return ResponseEntity with CheckoutResponse indicating order status.
     */
    @PreAuthorize("isAuthenticated()") // Only authenticated users can checkout
    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkoutCart(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CheckoutRequest checkoutRequest) {

        if (userDetails == null) {
            // This case should ideally be caught by @PreAuthorize, but good to have a fallback.
            logger.warn("Unauthorized checkout attempt: No user details.");
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new CheckoutResponse(null, "User not authenticated", false));
        }

        try {
            User currentUser = userService
                    .findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found after authentication."));

            Order newOrder = cartService.checkoutCart(currentUser, checkoutRequest);
            CheckoutResponse resp = new CheckoutResponse(
                    newOrder.getId(),
                    "Order placed successfully! Payment Status: " + newOrder.getPaymentStatus().name(),
                    true
            );
            logger.info("Order {} placed successfully for user {}. Payment Method: {}, Status: {}",
                    newOrder.getId(), currentUser.getUsername(), newOrder.getPaymentMethod(), newOrder.getPaymentStatus());
            return ResponseEntity.ok(resp);

        } catch (IllegalArgumentException ex) {
            logger.error("Checkout failed for user {}: {}", userDetails.getUsername(), ex.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new CheckoutResponse(null, ex.getMessage(), false));
        } catch (Exception ex) {
            logger.error("Server error during checkout for user {}: {}", userDetails.getUsername(), ex.getMessage(), ex);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CheckoutResponse(null, "Checkout failed: " + ex.getMessage(), false));
        }
    }
}