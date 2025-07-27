package com.example.handPick.service;

import com.example.handPick.model.AdminNotification;
import com.example.handPick.model.Order;
import com.example.handPick.model.User;
import com.example.handPick.repository.AdminNotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    @Autowired
    private AdminNotificationRepository notificationRepository;
    
    @Autowired
    private EmailNotificationService emailNotificationService;
    
    /**
     * Notify admin about new order placement
     */
    @Transactional
    public void notifyNewOrder(Order order, User customer) {
        try {
            // Create database notification
            AdminNotification notification = new AdminNotification();
            notification.setTitle("New Order Placed");
            notification.setMessage(String.format(
                "New order #%d placed by %s (%s) for ₹%.2f. Payment method: %s",
                order.getId(),
                getCustomerName(customer),
                customer.getMobileNumber(),
                order.getTotalAmount(),
                order.getPaymentMethod()
            ));
            notification.setType("ORDER_PLACED");
            notification.setOrderId(order.getId());
            notification.setCustomerName(getCustomerName(customer));
            notification.setCustomerMobile(customer.getMobileNumber());
            
            notificationRepository.save(notification);
            
            // Log to console (FREE option)
            logger.info("🔔 ADMIN NOTIFICATION: {}", notification.getMessage());
            
            // Send email notification
            emailNotificationService.sendNewOrderEmail(
                notification.getCustomerName(),
                notification.getCustomerMobile(),
                notification.getOrderId(),
                order.getTotalAmount().toString(),
                order.getPaymentMethod().name()
            );
            
            // TODO: Add WebSocket notification here for real-time updates
            
        } catch (Exception e) {
            logger.error("Failed to create admin notification for order {}", order.getId(), e);
        }
    }
    
    /**
     * Notify admin about order status change
     */
    @Transactional
    public void notifyOrderStatusChange(Order order, String oldStatus, String newStatus) {
        try {
            AdminNotification notification = new AdminNotification();
            notification.setTitle("Order Status Updated");
            notification.setMessage(String.format(
                "Order #%d status changed from %s to %s",
                order.getId(), oldStatus, newStatus
            ));
            notification.setType("ORDER_STATUS_CHANGED");
            notification.setOrderId(order.getId());
            notification.setCustomerName(getCustomerName(order.getUser()));
            notification.setCustomerMobile(order.getUser().getMobileNumber());
            
            notificationRepository.save(notification);
            logger.info("🔔 ADMIN NOTIFICATION: {}", notification.getMessage());
            
        } catch (Exception e) {
            logger.error("Failed to create status change notification for order {}", order.getId(), e);
        }
    }
    
    /**
     * Get unread notifications for admin
     */
    public List<AdminNotification> getUnreadNotifications() {
        return notificationRepository.findByIsReadFalseOrderByCreatedAtDesc();
    }
    
    /**
     * Get recent notifications (last 24 hours)
     */
    public List<AdminNotification> getRecentNotifications() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        return notificationRepository.findRecentNotifications(since);
    }
    
    /**
     * Mark notification as read
     */
    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
        });
    }
    
    /**
     * Mark all notifications as read
     */
    @Transactional
    public void markAllAsRead() {
        List<AdminNotification> unreadNotifications = notificationRepository.findByIsReadFalseOrderByCreatedAtDesc();
        unreadNotifications.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(unreadNotifications);
    }
    
    /**
     * Get notification count for admin dashboard
     */
    public long getUnreadCount() {
        return notificationRepository.countByIsReadFalse();
    }
    
    /**
     * Notify admin about order cancellation
     */
    @Transactional
    public void notifyOrderCancellation(Order order, User user, String reason) {
        try {
            AdminNotification notification = new AdminNotification();
            notification.setTitle("Order Cancelled");
            notification.setMessage(String.format(
                "Order #%d cancelled by %s (%s). Reason: %s. Amount: ₹%.2f",
                order.getId(),
                getCustomerName(user),
                user.getMobileNumber(),
                reason != null ? reason : "No reason provided",
                order.getTotalAmount()
            ));
            notification.setType("ORDER_CANCELLED");
            notification.setOrderId(order.getId());
            notification.setCustomerName(getCustomerName(user));
            notification.setCustomerMobile(user.getMobileNumber());
            
            notificationRepository.save(notification);
            
            // Log to console
            logger.info("🔔 ADMIN NOTIFICATION: {}", notification.getMessage());
            
            // Send email notification
            emailNotificationService.sendOrderCancellationEmail(
                notification.getCustomerName(),
                notification.getCustomerMobile(),
                notification.getOrderId(),
                order.getTotalAmount().toString(),
                reason
            );
            
        } catch (Exception e) {
            logger.error("Failed to create cancellation notification for order {}", order.getId(), e);
        }
    }
    
    private String getCustomerName(User customer) {
        if (customer.getFirstName() != null && customer.getLastName() != null) {
            return customer.getFirstName() + " " + customer.getLastName();
        } else if (customer.getFirstName() != null) {
            return customer.getFirstName();
        } else {
            return "Customer";
        }
    }
} 