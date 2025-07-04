package com.example.handPick.repository;

import com.example.handPick.model.Cart;
import com.example.handPick.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserAndStatus(User user, Cart.CartStatus status);
    // You might also need:
    // Optional<Cart> findByUser(User user);
}