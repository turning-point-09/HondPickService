package com.example.handPick.repository;

import com.example.handPick.model.Order;
import com.example.handPick.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser(User user);
    // You can add more query methods here, e.g., find by status, date range, etc.
}