package com.example.handPick.repository; // UPDATED

import com.example.handPick.model.CartItem; // UPDATED
import com.example.handPick.model.Cart; // UPDATED
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartAndProductId(Cart cart, Long productId);
}