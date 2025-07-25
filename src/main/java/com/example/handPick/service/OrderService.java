package com.example.handPick.service;

import com.example.handPick.dto.OrderFeedbackDto;
import com.example.handPick.model.Order;
import com.example.handPick.model.User;
import com.example.handPick.repository.OrderRepository;
import com.example.handPick.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    // Get paginated order history for a user
    public Page<Order> getOrdersForUser(String mobileNumber, Pageable pageable) {
        User user = userRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return orderRepository.findByUser(user, pageable);
    }

    // Get a single order by ID (with user check)
    public Order getOrderByIdForUser(Long orderId, String mobileNumber) {
        User user = userRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied for this order");
        }
        return order;
    }

    // Get a single order by ID (admin or for invoice)
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    // Save feedback for an order
    @Transactional
    public void saveOrderFeedback(Long orderId, OrderFeedbackDto feedbackDto, String mobileNumber) {
        User user = userRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied for this order");
        }
        order.setFeedbackComments(feedbackDto.getComments());
        order.setFeedbackOnTime(feedbackDto.getOnTime());
        orderRepository.save(order);
    }
}