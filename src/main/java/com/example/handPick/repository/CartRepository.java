package com.example.handPick.repository;

// Corrected package name to handpick
import com.example.handPick.model.Cart;
import com.example.handPick.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository; // Added Repository annotation

import java.util.Optional;
import java.util.UUID; // Import UUID for guestId

@Repository // Mark this as a Spring Data JPA repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    // Find an active cart by user
    Optional<Cart> findByUserAndStatus(User user, Cart.CartStatus status);

    // Find an active cart by guest ID (using UUID)
    Optional<Cart> findByGuestIdAndStatus(UUID guestId, Cart.CartStatus status);

    // Find any cart by user (useful for general user cart retrieval, regardless of status)
    Optional<Cart> findByUser(User user); // Ensure this method exists for CartService lookup

    // Find any cart by guest ID (using UUID, regardless of status)
    // This is the correct version for `findByGuestId` as per the `Cart` entity's `UUID guestId`.
    Optional<Cart> findByGuestId(UUID guestId);

    // Note: findByUserId(Long userId) might be redundant if findByUser is used,
    // or if you need to query by userId directly without loading the whole User object.
    // Keeping it for now as it was in your provided code, but consider if truly needed.
    Optional<Cart> findByUserId(Long userId);
}