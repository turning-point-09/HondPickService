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
import com.example.handPick.service.UserAddressService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private static final Logger logger = LoggerFactory.getLogger(CartController.class);

    private final CartService cartService;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final UserAddressService userAddressService;

    @Autowired
    public CartController(CartService cartService,
                          UserService userService,
                          JwtUtil jwtUtil,
                          UserAddressService userAddressService) {
        this.cartService = cartService;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.userAddressService = userAddressService;
    }

    /**
     * Retrieves the current user's shopping cart.
     * GET /api/cart
     */
    @GetMapping
    public ResponseEntity<CartDto> getCart(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User currentUser = userService.findByMobileNumber(userDetails.getUsername()).orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        CartDto cart = cartService.getCart(currentUser, null);
        return ResponseEntity.ok(cart);
    }

    /**
     * Retrieves the current user's shopping cart item count.
     * GET /api/cart/count
     */
    @GetMapping("/count")
    public ResponseEntity<Integer> getCartItemCount(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User currentUser = userService.findByMobileNumber(userDetails.getUsername()).orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        CartDto cart = cartService.getCart(currentUser, null);
        return ResponseEntity.ok(cart.getTotalItems());
    }

    /**
     * Adds an item to the current user's cart.
     * POST /api/cart/add
     */
    @PostMapping("/add")
    public ResponseEntity<CartDto> addItemToCart(
            @Valid @RequestBody AddToCartRequest reqDto,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User currentUser = userService.findByMobileNumber(userDetails.getUsername()).orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            CartDto updated = cartService.addItemToCart(currentUser, reqDto);
            logger.info("Item added to cart. Product ID: {}, Quantity: {}", reqDto.getProductId(), reqDto.getQuantity());
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException ex) {
            logger.error("Failed to add item to cart: {}", ex.getMessage());
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
     */
    @PutMapping("/item/{productId}")
    public ResponseEntity<CartDto> updateItemQuantity(
            @PathVariable Long productId,
            @Valid @RequestBody QuantityUpdateRequest reqDto,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User currentUser = userService.findByMobileNumber(userDetails.getUsername()).orElse(null);
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
     */
    @DeleteMapping("/item/{productId}")
    public ResponseEntity<CartDto> removeItem(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User currentUser = userService.findByMobileNumber(userDetails.getUsername()).orElse(null);
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
     * GET /api/cart/checkout/addresses
     * Get available addresses for checkout
     */
    @GetMapping("/checkout/addresses")
    public ResponseEntity<List<com.example.handPick.dto.UserAddressDto>> getCheckoutAddresses(
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User currentUser = userService.findByMobileNumber(userDetails.getUsername()).orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        List<com.example.handPick.dto.UserAddressDto> addresses = userAddressService.getUserAddresses(currentUser.getId());
        return ResponseEntity.ok(addresses);
    }

    /**
     * Handles the checkout process for an authenticated user.
     * POST /api/cart/checkout
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkoutCart(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CheckoutRequest checkoutRequest) {

        logger.info("=== CHECKOUT DEBUG ===");
        logger.info("User: {}", userDetails != null ? userDetails.getUsername() : "null");
        logger.info("CheckoutRequest: paymentMethod={}, selectedAddressId={}, newAddress={}", 
                   checkoutRequest != null ? checkoutRequest.getPaymentMethod() : "null",
                   checkoutRequest != null ? checkoutRequest.getSelectedAddressId() : "null",
                   checkoutRequest != null ? checkoutRequest.getNewAddress() : "null");
        logger.info("=== END CHECKOUT DEBUG ===");

        if (userDetails == null) {
            logger.warn("Unauthorized checkout attempt: No user details.");
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new CheckoutResponse(null, "User not authenticated", false));
        }

        try {
            User currentUser = userService
                    .findByMobileNumber(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found during checkout."));

            Order order = cartService.checkoutCart(currentUser, checkoutRequest);
            logger.info("Checkout successful for user {}. Order ID: {}", currentUser.getMobileNumber(), order.getId());

            return ResponseEntity.ok(new CheckoutResponse(order.getId(), "Order placed successfully!", true));

        } catch (IllegalArgumentException ex) {
            logger.error("Checkout failed due to invalid request: {}", ex.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new CheckoutResponse(null, ex.getMessage(), false));
        } catch (Exception ex) {
            logger.error("Server error during checkout: {}", ex.getMessage(), ex);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CheckoutResponse(null, "Server error during checkout", false));
        }
    }
}