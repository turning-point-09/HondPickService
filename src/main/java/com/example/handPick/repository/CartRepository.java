package com.example.handPick.repository;

import com.example.handPick.model.Cart;
import com.example.handPick.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserAndStatus(User user, Cart.CartStatus status);
    Optional<Cart> findByGuestIdAndStatus(UUID guestId, Cart.CartStatus status);
    Optional<Cart> findByUserId(Long userId); // For deleting all carts if needed

    // You only need one of these methods
    Optional<Cart> findByGuestId(UUID guestId); // For finding any guest cart

    // The duplicated one below needs to be removed:
    // Optional<Cart> findByGuestId(UUID guestId); // <--- REMOVE THIS LINE
}