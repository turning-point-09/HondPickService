package com.example.handPick.repository;

import com.example.handPick.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    // Basic CRUD provided by JpaRepository
}