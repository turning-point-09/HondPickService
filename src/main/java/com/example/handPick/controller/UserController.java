package com.example.handPick.controller;

import com.example.handPick.dto.AddressDto;
import com.example.handPick.dto.UserDto;
import com.example.handPick.dto.UserProfileResponse;
import com.example.handPick.model.User;
import com.example.handPick.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    /**
     * GET /api/users/me/id
     * Get the ID of the currently authenticated user.
     */
    @GetMapping("/me/id")
    public ResponseEntity<Long> getCurrentUserId(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            logger.warn("Unauthorized access attempt to /me/id: No user details.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            User currentUser = userService.findByMobileNumber(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found for authenticated principal."));
            logger.info("Fetched user ID {} for user {}", currentUser.getId(), currentUser.getMobileNumber());
            return ResponseEntity.ok(currentUser.getId());
        } catch (Exception e) {
            logger.error("Error fetching user ID for {}: {}", userDetails.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/users/me/address
     * Get the shipping address of the currently authenticated user.
     */
    @GetMapping("/me/address")
    public ResponseEntity<AddressDto> getCurrentUserAddress(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            logger.warn("Unauthorized access attempt to /me/address: No user details.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            User currentUser = userService.findByMobileNumber(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found for authenticated principal."));

            if (currentUser.getAddress() != null) {
                AddressDto addressDto = new AddressDto(
                        currentUser.getAddress().getId(),
                        currentUser.getAddress().getStreet(),
                        currentUser.getAddress().getCity(),
                        currentUser.getAddress().getState(),
                        currentUser.getAddress().getPostalCode(),
                        currentUser.getAddress().getCountry()
                );
                logger.info("Fetched address for user {}", currentUser.getMobileNumber());
                return ResponseEntity.ok(addressDto);
            } else {
                logger.info("No address found for user {}", currentUser.getMobileNumber());
                return ResponseEntity.noContent().build();
            }
        } catch (Exception e) {
            logger.error("Error fetching user address for {}: {}", userDetails.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * PUT /api/users/me/address
     * Update or create the shipping address for the currently authenticated user.
     */
    @PutMapping("/me/address")
    public ResponseEntity<AddressDto> updateCurrentUserAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AddressDto addressDto) {
        if (userDetails == null) {
            logger.warn("Unauthorized access attempt to update /me/address: No user details.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            User currentUser = userService.findByMobileNumber(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found for authenticated principal."));

            com.example.handPick.model.Address updatedAddressEntity = userService.updateOrCreateUserAddress(currentUser.getId(), addressDto);

            AddressDto updatedAddressDto = new AddressDto(
                    updatedAddressEntity.getId(),
                    updatedAddressEntity.getStreet(),
                    updatedAddressEntity.getCity(),
                    updatedAddressEntity.getState(),
                    updatedAddressEntity.getPostalCode(),
                    updatedAddressEntity.getCountry()
            );

            logger.info("Address updated successfully for user {}", currentUser.getMobileNumber());
            return ResponseEntity.ok(updatedAddressDto);
        } catch (Exception e) {
            logger.error("Error updating user address for {}: {}", userDetails.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
        }
        String mobileNumber = userDetails.getUsername();
        return userService.findByMobileNumber(mobileNumber)
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(new UserProfileResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getRole(),
                        user.getMobileNumber()
                )))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found"));
    }

    /**
     * Allows a user to delete their own account (soft delete).
     * @return Success message.
     */
    @DeleteMapping("/account")
    public ResponseEntity<String> deleteOwnAccount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String mobileNumber = authentication.getName();
        
        // Find user by mobile number
        User user = userService.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Delete the account (soft delete)
        userService.deleteUserAccount(user.getId());
        
        return ResponseEntity.ok("Account deleted successfully");
    }

    /**
     * Gets the current user's profile information.
     * @return User profile data.
     */
    @GetMapping("/profile")
    public ResponseEntity<UserDto> getCurrentUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String mobileNumber = authentication.getName();
        
        User user = userService.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return ResponseEntity.ok(userService.convertToDto(user));
    }
}