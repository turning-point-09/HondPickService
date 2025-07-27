package com.example.handPick.service;

import com.example.handPick.dto.OrderFeedbackDto;
import com.example.handPick.model.Order;
import com.example.handPick.model.OrderItem;
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
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private ProductService productService;

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
            String oldStatus = order.getStatus().name();
            order.setStatus(newStatus);
            Order savedOrder = orderRepository.save(order);
            
            // Notify admin about status change
            notificationService.notifyOrderStatusChange(savedOrder, oldStatus, newStatus.name());
            
            return savedOrder;
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid order status: " + status);
        }
    }

    // Cancel order (user can cancel their own orders, admin can cancel any)
    @Transactional
    public Order cancelOrder(Long orderId, String mobileNumber, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        // Check if user can cancel this order
        User user = userRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // User can only cancel their own orders, unless they're admin
        if (!order.getUser().getId().equals(user.getId()) && !"ADMIN".equals(user.getRole())) {
            throw new RuntimeException("Access denied: You can only cancel your own orders");
        }
        
        // Check if order can be cancelled
        if (order.getStatus() != Order.OrderStatus.PENDING && 
            order.getStatus() != Order.OrderStatus.COMPLETED) {
            throw new RuntimeException("Order cannot be cancelled. Current status: " + order.getStatus());
        }
        
        String oldStatus = order.getStatus().name();
        order.setStatus(Order.OrderStatus.CANCELLED);
        
        // Restore stock quantities
        restoreStockForCancelledOrder(order);
        
        // Process refund if payment was made
        processRefundForCancelledOrder(order);
        
        Order savedOrder = orderRepository.save(order);
        
        // Notify admin about order cancellation
        notificationService.notifyOrderCancellation(savedOrder, user, reason);
        
        return savedOrder;
    }
    
    // Restore stock quantities when order is cancelled
    private void restoreStockForCancelledOrder(Order order) {
        for (OrderItem item : order.getItems()) {
            try {
                productService.restoreStock(item.getProductId(), item.getQuantity());
            } catch (Exception e) {
                // Log error but don't fail the cancellation
                System.err.println("Failed to restore stock for product " + item.getProductId() + ": " + e.getMessage());
            }
        }
    }
    
    // Process refund for cancelled order
    private void processRefundForCancelledOrder(Order order) {
        if (order.getPaymentStatus() == Order.PaymentStatus.COMPLETED) {
            // Update payment status to refunded
            order.setPaymentStatus(Order.PaymentStatus.REFUNDED);
            
            // Here you would integrate with your payment gateway to process the actual refund
            // For now, we just update the status
            System.out.println("Processing refund for order " + order.getId() + " amount: " + order.getTotalAmount());
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