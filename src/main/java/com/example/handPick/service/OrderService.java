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

import java.util.List;

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

    // Get all orders (paged) for admin
    public Page<Order> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable);
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

    // Update order status (admin only)
    @Transactional
    public Order updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        try {
            Order.OrderStatus newStatus = Order.OrderStatus.valueOf(status.toUpperCase());
            order.setStatus(newStatus);
            return orderRepository.save(order);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid order status: " + status);
        }
    }

    // Convert Order entity to OrderDto for clean API responses
    public com.example.handPick.dto.OrderDto convertToDto(com.example.handPick.model.Order order) {
        if (order == null) return null;
        com.example.handPick.dto.OrderDto dto = new com.example.handPick.dto.OrderDto();
        dto.setId(order.getId());
        // Convert items
        if (order.getItems() != null) {
            List<com.example.handPick.dto.OrderItemDto> itemDtos = order.getItems().stream().map(item -> {
                com.example.handPick.dto.OrderItemDto itemDto = new com.example.handPick.dto.OrderItemDto();
                itemDto.setId(item.getId());
                itemDto.setProductId(item.getProductId());
                itemDto.setProductName(item.getProductName());
                itemDto.setQuantity(item.getQuantity());
                itemDto.setUnitPrice(item.getUnitPrice());
                itemDto.setTotalPrice(item.getSubtotal());
                return itemDto;
            }).collect(java.util.stream.Collectors.toList());
            dto.setItems(itemDtos);
        }
        dto.setTotalAmount(order.getTotalAmount());
        dto.setStatus(order.getStatus() != null ? order.getStatus().name() : null);
        dto.setPaymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null);
        dto.setPaymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null);
        dto.setTransactionId(order.getTransactionId());
        // Convert shipping address
        if (order.getShippingAddress() != null) {
            com.example.handPick.dto.AddressDto addressDto = new com.example.handPick.dto.AddressDto();
            addressDto.setStreet(order.getShippingAddress().getStreet());
            addressDto.setCity(order.getShippingAddress().getCity());
            addressDto.setState(order.getShippingAddress().getState());
            addressDto.setPostalCode(order.getShippingAddress().getPostalCode());
            addressDto.setCountry(order.getShippingAddress().getCountry());
            dto.setShippingAddress(addressDto);
        }
        dto.setOrderDate(order.getOrderDate());
        return dto;
    }
}