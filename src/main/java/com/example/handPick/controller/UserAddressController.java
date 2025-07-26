package com.example.handPick.controller;

import com.example.handPick.dto.UserAddressDto;
import com.example.handPick.model.User;
import com.example.handPick.service.UserAddressService;
import com.example.handPick.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/addresses")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class UserAddressController {

    private static final Logger logger = LoggerFactory.getLogger(UserAddressController.class);

    @Autowired
    private UserAddressService userAddressService;

    @Autowired
    private UserService userService;

    /**
     * GET /api/user/addresses
     * Get all addresses for the current user
     */
    @GetMapping
    public ResponseEntity<List<UserAddressDto>> getUserAddresses(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            logger.warn("Unauthorized access attempt to /addresses: No user details.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            User currentUser = userService.findByMobileNumber(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found for authenticated principal."));
            
            List<UserAddressDto> addresses = userAddressService.getUserAddresses(currentUser.getId());
            logger.info("Fetched {} addresses for user {}", addresses.size(), currentUser.getMobileNumber());
            return ResponseEntity.ok(addresses);
        } catch (Exception e) {
            logger.error("Error fetching addresses for {}: {}", userDetails.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/user/addresses/default
     * Get default address for the current user
     */
    @GetMapping("/default")
    public ResponseEntity<UserAddressDto> getDefaultAddress(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            logger.warn("Unauthorized access attempt to /addresses/default: No user details.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            User currentUser = userService.findByMobileNumber(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found for authenticated principal."));
            
            return userAddressService.getDefaultAddress(currentUser.getId())
                    .map(address -> {
                        logger.info("Fetched default address for user {}", currentUser.getMobileNumber());
                        return ResponseEntity.ok(address);
                    })
                    .orElse(ResponseEntity.noContent().build());
        } catch (Exception e) {
            logger.error("Error fetching default address for {}: {}", userDetails.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/user/addresses/{addressId}
     * Get specific address by ID
     */
    @GetMapping("/{addressId}")
    public ResponseEntity<UserAddressDto> getAddressById(
            @PathVariable Long addressId,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            logger.warn("Unauthorized access attempt to /addresses/{}: No user details.", addressId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            User currentUser = userService.findByMobileNumber(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found for authenticated principal."));
            
            UserAddressDto address = userAddressService.getAddressById(addressId, currentUser.getId());
            logger.info("Fetched address {} for user {}", addressId, currentUser.getMobileNumber());
            return ResponseEntity.ok(address);
        } catch (Exception e) {
            logger.error("Error fetching address {} for {}: {}", addressId, userDetails.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /api/user/addresses
     * Add new address for the current user
     */
    @PostMapping
    public ResponseEntity<UserAddressDto> addAddress(
            @Valid @RequestBody UserAddressDto addressDto,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            logger.warn("Unauthorized access attempt to add address: No user details.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            User currentUser = userService.findByMobileNumber(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found for authenticated principal."));
            
            UserAddressDto savedAddress = userAddressService.addAddress(currentUser.getId(), addressDto);
            logger.info("Added new address for user {}", currentUser.getMobileNumber());
            return ResponseEntity.status(HttpStatus.CREATED).body(savedAddress);
        } catch (Exception e) {
            logger.error("Error adding address for {}: {}", userDetails.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * PUT /api/user/addresses/{addressId}
     * Update existing address
     */
    @PutMapping("/{addressId}")
    public ResponseEntity<UserAddressDto> updateAddress(
            @PathVariable Long addressId,
            @Valid @RequestBody UserAddressDto addressDto,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            logger.warn("Unauthorized access attempt to update address {}: No user details.", addressId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            User currentUser = userService.findByMobileNumber(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found for authenticated principal."));
            
            UserAddressDto updatedAddress = userAddressService.updateAddress(addressId, currentUser.getId(), addressDto);
            logger.info("Updated address {} for user {}", addressId, currentUser.getMobileNumber());
            return ResponseEntity.ok(updatedAddress);
        } catch (Exception e) {
            logger.error("Error updating address {} for {}: {}", addressId, userDetails.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * DELETE /api/user/addresses/{addressId}
     * Delete address
     */
    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable Long addressId,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            logger.warn("Unauthorized access attempt to delete address {}: No user details.", addressId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            User currentUser = userService.findByMobileNumber(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found for authenticated principal."));
            
            userAddressService.deleteAddress(addressId, currentUser.getId());
            logger.info("Deleted address {} for user {}", addressId, currentUser.getMobileNumber());
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Error deleting address {} for {}: {}", addressId, userDetails.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * PATCH /api/user/addresses/{addressId}/default
     * Set address as default
     */
    @PatchMapping("/{addressId}/default")
    public ResponseEntity<UserAddressDto> setDefaultAddress(
            @PathVariable Long addressId,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            logger.warn("Unauthorized access attempt to set default address {}: No user details.", addressId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            User currentUser = userService.findByMobileNumber(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found for authenticated principal."));
            
            UserAddressDto defaultAddress = userAddressService.setDefaultAddress(addressId, currentUser.getId());
            logger.info("Set address {} as default for user {}", addressId, currentUser.getMobileNumber());
            return ResponseEntity.ok(defaultAddress);
        } catch (Exception e) {
            logger.error("Error setting default address {} for {}: {}", addressId, userDetails.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
} 