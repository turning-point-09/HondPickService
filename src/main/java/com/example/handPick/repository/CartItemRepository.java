package com.example.handPick.repository;

import com.example.handPick.model.CartItem;
import com.example.handPick.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartAndProductId(Cart cart, Long productId);
}