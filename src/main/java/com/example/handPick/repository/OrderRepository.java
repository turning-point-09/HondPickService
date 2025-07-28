package com.example.handPick.repository;

import com.example.handPick.model.Order;
import com.example.handPick.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUser(User user);

    // Add this paginated method for the service
    Page<Order> findByUser(User user, Pageable pageable);
    
    // Filter orders by user and date range
    @Query("SELECT o FROM Order o WHERE o.user = :user AND o.orderDate >= :startDate AND o.orderDate <= :endDate")
    Page<Order> findByUserAndOrderDateBetween(@Param("user") User user, 
                                             @Param("startDate") LocalDateTime startDate, 
                                             @Param("endDate") LocalDateTime endDate, 
                                             Pageable pageable);
    
    // Filter orders by user and status
    @Query("SELECT o FROM Order o WHERE o.user = :user AND o.status = :status")
    Page<Order> findByUserAndStatus(@Param("user") User user, 
                                   @Param("status") Order.OrderStatus status, 
                                   Pageable pageable);
    
    // Filter orders by user, status and date range
    @Query("SELECT o FROM Order o WHERE o.user = :user AND o.status = :status AND o.orderDate >= :startDate AND o.orderDate <= :endDate")
    Page<Order> findByUserAndStatusAndOrderDateBetween(@Param("user") User user, 
                                                      @Param("status") Order.OrderStatus status,
                                                      @Param("startDate") LocalDateTime startDate, 
                                                      @Param("endDate") LocalDateTime endDate, 
                                                      Pageable pageable);
    
    // Admin: Filter all orders by date range
    @Query("SELECT o FROM Order o WHERE o.orderDate >= :startDate AND o.orderDate <= :endDate")
    Page<Order> findByOrderDateBetween(@Param("startDate") LocalDateTime startDate, 
                                      @Param("endDate") LocalDateTime endDate, 
                                      Pageable pageable);
    
    // Admin: Filter all orders by status
    @Query("SELECT o FROM Order o WHERE o.status = :status")
    Page<Order> findByStatus(@Param("status") Order.OrderStatus status, Pageable pageable);
    
    // Admin: Filter all orders by status and date range
    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.orderDate >= :startDate AND o.orderDate <= :endDate")
    Page<Order> findByStatusAndOrderDateBetween(@Param("status") Order.OrderStatus status,
                                               @Param("startDate") LocalDateTime startDate, 
                                               @Param("endDate") LocalDateTime endDate, 
                                               Pageable pageable);
    
    // Calculate monthly revenue from delivered orders
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = 'DELIVERED' AND o.orderDate >= :startDate AND o.orderDate < :endDate")
    BigDecimal calculateMonthlyRevenue(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Count delivered orders for the month
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'DELIVERED' AND o.orderDate >= :startDate AND o.orderDate < :endDate")
    long countDeliveredOrdersForMonth(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Calculate total revenue from all delivered orders
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status = 'DELIVERED'")
    BigDecimal calculateTotalRevenue();
    
    // Count total delivered orders
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'DELIVERED'")
    long countTotalDeliveredOrders();

    // Count pending orders for the month
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'PENDING' AND o.orderDate >= :startDate AND o.orderDate < :endDate")
    long countPendingOrdersForMonth(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Count total pending orders
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'PENDING'")
    long countTotalPendingOrders();

    // Calculate monthly profit from delivered orders
    @Query("SELECT COALESCE(SUM(oi.subtotal - (oi.unitPrice * oi.quantity * (SELECT p.purchasePrice FROM Product p WHERE p.id = oi.productId) / p.price)), 0) FROM Order o JOIN o.items oi JOIN Product p ON p.id = oi.productId WHERE o.status = 'DELIVERED' AND o.orderDate >= :startDate AND o.orderDate < :endDate")
    BigDecimal calculateMonthlyProfit(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Calculate total profit from all delivered orders
    @Query("SELECT COALESCE(SUM(oi.subtotal - (oi.unitPrice * oi.quantity * (SELECT p.purchasePrice FROM Product p WHERE p.id = oi.productId) / p.price)), 0) FROM Order o JOIN o.items oi JOIN Product p ON p.id = oi.productId WHERE o.status = 'DELIVERED'")
    BigDecimal calculateTotalProfit();
}