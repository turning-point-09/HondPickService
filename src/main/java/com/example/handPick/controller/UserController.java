package com.example.handPick.controller;

import com.example.handPick.dto.AddressDto;
import com.example.handPick.model.User;
import com.example.handPick.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    /**
     * Endpoint to get the ID of the currently authenticated user.
     * This is needed by the frontend because the JWT token is HttpOnly.
     * GET /api/users/me/id
     * @param userDetails The authenticated user's details provided by Spring Security.
     * @return ResponseEntity with the user's ID if authenticated, or 401 Unauthorized.
     */
//    @PreAuthorize("isAuthenticated()") // Ensure only authenticated users can access this
    @GetMapping("/me/id")
    public ResponseEntity<Long> getCurrentUserId(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            logger.warn("Unauthorized access attempt to /me/id: No user details. {}",userDetails);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            User currentUser = userService.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found for authenticated principal."));
            logger.info("Fetched user ID {} for user {}", currentUser.getId(), currentUser.getUsername());
            return ResponseEntity.ok(currentUser.getId());
        } catch (Exception e) {
            logger.error("Error fetching user ID for {}: {}", userDetails.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Endpoint to get the shipping address of the currently authenticated user.
     * GET /api/users/me/address
     * @param userDetails The authenticated user's details.
     * @return ResponseEntity with AddressDto if found, or 204 No Content if no address.
     */
//    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me/address")
    public ResponseEntity<AddressDto> getCurrentUserAddress(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            logger.warn("Unauthorized access attempt to /me/address: No user details.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            User currentUser = userService.findByUsername(userDetails.getUsername())
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
                logger.info("Fetched address for user {}", currentUser.getUsername());
                return ResponseEntity.ok(addressDto);
            } else {
                logger.info("No address found for user {}", currentUser.getUsername());
                return ResponseEntity.noContent().build(); // 204 No Content if no address
            }
        } catch (Exception e) {
            logger.error("Error fetching user address for {}: {}", userDetails.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Endpoint to update or create the shipping address for the currently authenticated user.
     * PUT /api/users/me/address
     * @param userDetails The authenticated user's details.
     * @param addressDto The AddressDto containing the new or updated address details.
     * @return ResponseEntity with the updated AddressDto.
     */
//    @PreAuthorize("isAuthenticated()")
    @PutMapping("/me/address")
    public ResponseEntity<AddressDto> updateCurrentUserAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AddressDto addressDto) {
        if (userDetails == null) {
            logger.warn("Unauthorized access attempt to update /me/address: No user details.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            User currentUser = userService.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found for authenticated principal."));

            // Call the service method which returns an Address entity
            com.example.handPick.model.Address updatedAddressEntity = userService.updateOrCreateUserAddress(currentUser.getId(), addressDto);

            // Convert the Address entity back to an AddressDto for the response
            AddressDto updatedAddressDto = new AddressDto(
                    updatedAddressEntity.getId(),
                    updatedAddressEntity.getStreet(),
                    updatedAddressEntity.getCity(),
                    updatedAddressEntity.getState(),
                    updatedAddressEntity.getPostalCode(),
                    updatedAddressEntity.getCountry()
            );

            logger.info("Address updated successfully for user {}", currentUser.getUsername());
            return ResponseEntity.ok(updatedAddressDto);
        } catch (Exception e) {
            logger.error("Error updating user address for {}: {}", userDetails.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
