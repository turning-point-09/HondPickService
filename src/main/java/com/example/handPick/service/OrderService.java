package com.example.handPick.service;

import com.example.handPick.dto.OrderFeedbackDto;
import com.example.handPick.model.Order;
import com.example.handPick.model.OrderItem;
import com.example.handPick.model.User;
import com.example.handPick.repository.OrderRepository;
import com.example.handPick.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
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

    // Get orders for user with filtering and sorting
    public Page<Order> getOrdersForUserWithFilters(String mobileNumber, 
                                                   String status, 
                                                   LocalDateTime startDate, 
                                                   LocalDateTime endDate, 
                                                   String sortBy, 
                                                   String sortDirection, 
                                                   Pageable pageable) {
        User user = userRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Apply sorting
        Sort sort = createSort(sortBy, sortDirection);
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        
        // Apply filters
        if (status != null && startDate != null && endDate != null) {
            Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
            return orderRepository.findByUserAndStatusAndOrderDateBetween(user, orderStatus, startDate, endDate, sortedPageable);
        } else if (status != null) {
            Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
            return orderRepository.findByUserAndStatus(user, orderStatus, sortedPageable);
        } else if (startDate != null && endDate != null) {
            return orderRepository.findByUserAndOrderDateBetween(user, startDate, endDate, sortedPageable);
        } else {
            return orderRepository.findByUser(user, sortedPageable);
        }
    }

    // Get orders for user filtered by month
    public Page<Order> getOrdersForUserByMonth(String mobileNumber, int year, int month, String sortBy, String sortDirection, Pageable pageable) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime startDate = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endDate = yearMonth.atEndOfMonth().atTime(23, 59, 59);
        
        return getOrdersForUserWithFilters(mobileNumber, null, startDate, endDate, sortBy, sortDirection, pageable);
    }

    // Get all orders for admin with filtering and sorting
    public Page<Order> getAllOrdersWithFilters(String status, 
                                              LocalDateTime startDate, 
                                              LocalDateTime endDate, 
                                              String sortBy, 
                                              String sortDirection, 
                                              Pageable pageable) {
        // Apply sorting
        Sort sort = createSort(sortBy, sortDirection);
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        
        // Apply filters
        if (status != null && startDate != null && endDate != null) {
            Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
            return orderRepository.findByStatusAndOrderDateBetween(orderStatus, startDate, endDate, sortedPageable);
        } else if (status != null) {
            Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
            return orderRepository.findByStatus(orderStatus, sortedPageable);
        } else if (startDate != null && endDate != null) {
            return orderRepository.findByOrderDateBetween(startDate, endDate, sortedPageable);
        } else {
            return orderRepository.findAll(sortedPageable);
        }
    }

    // Get all orders for admin filtered by month
    public Page<Order> getAllOrdersByMonth(int year, int month, String sortBy, String sortDirection, Pageable pageable) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime startDate = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endDate = yearMonth.atEndOfMonth().atTime(23, 59, 59);
        
        return getAllOrdersWithFilters(null, startDate, endDate, sortBy, sortDirection, pageable);
    }

    // Create Sort object based on sortBy and sortDirection
    private Sort createSort(String sortBy, String sortDirection) {
        if (sortBy == null || sortBy.trim().isEmpty()) {
            sortBy = "orderDate"; // Default sort by order date
        }
        
        Sort.Direction direction = Sort.Direction.DESC; // Default to descending
        if (sortDirection != null && sortDirection.equalsIgnoreCase("asc")) {
            direction = Sort.Direction.ASC;
        }
        
        // Handle multiple sort fields (comma-separated)
        if (sortBy.contains(",")) {
            String[] sortFields = sortBy.split(",");
            Sort combinedSort = null;
            
            for (int i = 0; i < sortFields.length; i++) {
                String field = sortFields[i].trim();
                String fieldDirection = direction.name();
                
                // Check if field has its own direction (e.g., "orderDate:asc")
                if (field.contains(":")) {
                    String[] parts = field.split(":");
                    field = parts[0].trim();
                    fieldDirection = parts[1].trim().toUpperCase();
                }
                
                Sort currentSort = createSingleSort(field, fieldDirection);
                
                if (combinedSort == null) {
                    combinedSort = currentSort;
                } else {
                    combinedSort = combinedSort.and(currentSort);
                }
            }
            
            return combinedSort;
        } else {
            return createSingleSort(sortBy, direction.name());
        }
    }
    
    // Create a single Sort object for a specific field
    private Sort createSingleSort(String sortBy, String direction) {
        Sort.Direction sortDirection = Sort.Direction.DESC; // Default to descending
        if (direction != null && direction.equalsIgnoreCase("ASC")) {
            sortDirection = Sort.Direction.ASC;
        }
        
        switch (sortBy.toLowerCase().trim()) {
            // Order date sorting (multiple aliases)
            case "orderdate":
            case "order_date":
            case "date":
            case "created":
            case "createdat":
            case "created_at":
                return Sort.by(sortDirection, "orderDate");
            
            // Total amount sorting (multiple aliases)
            case "totalamount":
            case "total_amount":
            case "amount":
            case "price":
            case "total":
                return Sort.by(sortDirection, "totalAmount");
            
            // Order status sorting
            case "status":
            case "orderstatus":
            case "order_status":
                return Sort.by(sortDirection, "status");
            
            // Payment method sorting
            case "paymentmethod":
            case "payment_method":
            case "method":
            case "paymethod":
                return Sort.by(sortDirection, "paymentMethod");
            
            // Payment status sorting
            case "paymentstatus":
            case "payment_status":
            case "paystatus":
            case "paid":
                return Sort.by(sortDirection, "paymentStatus");
            
            // Order ID sorting
            case "id":
            case "orderid":
            case "order_id":
                return Sort.by(sortDirection, "id");
            
            // Transaction ID sorting
            case "transactionid":
            case "transaction_id":
            case "txn":
            case "txnid":
                return Sort.by(sortDirection, "transactionId");
            
            // Feedback sorting
            case "feedback":
            case "feedbackcomments":
            case "feedback_comments":
            case "comments":
                return Sort.by(sortDirection, "feedbackComments");
            
            case "feedbackontime":
            case "feedback_on_time":
            case "ontime":
            case "on_time":
                return Sort.by(sortDirection, "feedbackOnTime");
            
            // User-related sorting (for admin)
            case "user":
            case "userid":
            case "user_id":
            case "customer":
            case "customerid":
            case "customer_id":
                return Sort.by(sortDirection, "user.id");
            
            case "username":
            case "user_name":
            case "customername":
            case "customer_name":
                return Sort.by(sortDirection, "user.firstName");
            
            case "mobile":
            case "mobilenumber":
            case "mobile_number":
            case "phone":
            case "phonenumber":
            case "phone_number":
                return Sort.by(sortDirection, "user.mobileNumber");
            
            // Shipping address sorting
            case "city":
            case "shippingcity":
            case "shipping_city":
                return Sort.by(sortDirection, "shippingAddress.city");
            
            case "state":
            case "shippingstate":
            case "shipping_state":
                return Sort.by(sortDirection, "shippingAddress.state");
            
            case "postalcode":
            case "postal_code":
            case "zip":
            case "zipcode":
            case "zip_code":
                return Sort.by(sortDirection, "shippingAddress.postalCode");
            
            // Default fallback
            default:
                return Sort.by(sortDirection, "orderDate");
        }
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