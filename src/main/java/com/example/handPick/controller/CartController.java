package com.example.handPick.controller;

import com.example.handPick.config.JwtUtil;
import com.example.handPick.dto.AddToCartRequest;
import com.example.handPick.dto.CartDto;
import com.example.handPick.dto.CheckoutResponse;
import com.example.handPick.dto.QuantityUpdateRequest;
import com.example.handPick.model.Order;
import com.example.handPick.model.User;
import com.example.handPick.service.CartService;
import com.example.handPick.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

@RestController
@RequestMapping("/api/cart")
public class CartController {

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

    private UUID getValidGuestIdFromCookie(HttpServletRequest req, HttpServletResponse res) {
        return Arrays.stream(Optional.ofNullable(req.getCookies()).orElse(new Cookie[0]))
                .filter(c -> "guest_token".equals(c.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .flatMap(token -> {
                    if (jwtUtil.validateGuestToken(token)) {
                        return Optional.ofNullable(jwtUtil.extractGuestId(token));
                    } else {
                        Cookie expired = new Cookie("guest_token", "");
                        expired.setPath("/");
                        expired.setHttpOnly(true);
                        expired.setMaxAge(0);
                        res.addCookie(expired);
                        return Optional.empty();
                    }
                })
                .orElse(null);
    }

    @GetMapping
    public ResponseEntity<CartDto> getCart(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest req,
            HttpServletResponse res) {

        User currentUser = Optional.ofNullable(userDetails)
                .flatMap(u -> userService.findByUsername(u.getUsername()))
                .orElse(null);

        UUID guestId = getValidGuestIdFromCookie(req, res);
        if (currentUser == null && guestId == null) {
            guestId = UUID.randomUUID();
            String token = jwtUtil.generateGuestToken(guestId);
            Cookie cookie = new Cookie("guest_token", token);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge((int) (jwtUtil.getGuestExpiration() / 1000));
            res.addCookie(cookie);
        }

        CartDto cart = cartService.getCart(currentUser, guestId);
        return ResponseEntity.ok(cart);
    }

    @PostMapping("/add")
    public ResponseEntity<CartDto> addItemToCart(
            @RequestBody AddToCartRequest reqDto,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest req,
            HttpServletResponse res) {

        User currentUser = Optional.ofNullable(userDetails)
                .flatMap(u -> userService.findByUsername(u.getUsername()))
                .orElse(null);

        UUID guestId = getValidGuestIdFromCookie(req, res);
        if (currentUser == null && guestId == null) {
            guestId = UUID.randomUUID();
            String token = jwtUtil.generateGuestToken(guestId);
            Cookie cookie = new Cookie("guest_token", token);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge((int) (jwtUtil.getGuestExpiration() / 1000));
            res.addCookie(cookie);
        }

        try {
            CartDto updated = cartService.addItemToCart(currentUser, guestId, reqDto);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException ex) {
            CartDto error = new CartDto(
                    null,
                    Collections.emptyList(),
                    BigDecimal.ZERO,
                    0,
                    null,
                    ex.getMessage()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception ex) {
            CartDto error = new CartDto(
                    null,
                    Collections.emptyList(),
                    BigDecimal.ZERO,
                    0,
                    null,
                    "Server error: " + ex.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PutMapping("/item/{productId}")
    public ResponseEntity<CartDto> updateItemQuantity(
            @PathVariable Long productId,
            @RequestBody QuantityUpdateRequest reqDto,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest req,
            HttpServletResponse res) {

        User currentUser = Optional.ofNullable(userDetails)
                .flatMap(u -> userService.findByUsername(u.getUsername()))
                .orElse(null);

        UUID guestId = getValidGuestIdFromCookie(req, res);

        try {
            CartDto updated = cartService.updateItemQuantity(
                    currentUser, guestId, productId, reqDto.getQuantity());
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException ex) {
            CartDto error = new CartDto(
                    null,
                    Collections.emptyList(),
                    BigDecimal.ZERO,
                    0,
                    null,
                    ex.getMessage()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception ex) {
            CartDto error = new CartDto(
                    null,
                    Collections.emptyList(),
                    BigDecimal.ZERO,
                    0,
                    null,
                    "Server error: " + ex.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @DeleteMapping("/item/{productId}")
    public ResponseEntity<CartDto> removeItem(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest req,
            HttpServletResponse res) {

        User currentUser = Optional.ofNullable(userDetails)
                .flatMap(u -> userService.findByUsername(u.getUsername()))
                .orElse(null);

        UUID guestId = getValidGuestIdFromCookie(req, res);

        try {
            CartDto updated = cartService.removeItemFromCart(
                    currentUser, guestId, productId);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException ex) {
            CartDto error = new CartDto(
                    null,
                    Collections.emptyList(),
                    BigDecimal.ZERO,
                    0,
                    null,
                    ex.getMessage()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception ex) {
            CartDto error = new CartDto(
                    null,
                    Collections.emptyList(),
                    BigDecimal.ZERO,
                    0,
                    null,
                    "Server error: " + ex.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkoutCart(
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new CheckoutResponse(null, "User not authenticated", false));
        }

        try {
            User currentUser = userService
                    .findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Order newOrder = cartService.checkoutCart(currentUser);
            CheckoutResponse resp = new CheckoutResponse(
                    newOrder.getId(),
                    "Order placed successfully!",
                    true
            );
            return ResponseEntity.ok(resp);

        } catch (IllegalArgumentException ex) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new CheckoutResponse(null, ex.getMessage(), false));
        } catch (Exception ex) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CheckoutResponse(null, "Checkout failed: " + ex.getMessage(), false));
        }
    }
}