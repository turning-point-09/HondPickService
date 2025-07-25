package com.example.handPick.repository;

import com.example.handPick.model.Order;
import com.example.handPick.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser(User user);

    // Add this paginated method for the service
    Page<Order> findByUser(User user, Pageable pageable);
}