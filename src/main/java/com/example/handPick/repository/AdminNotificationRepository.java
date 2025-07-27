package com.example.handPick.repository;

import com.example.handPick.model.AdminNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AdminNotificationRepository extends JpaRepository<AdminNotification, Long> {
    
    // Find unread notifications
    List<AdminNotification> findByIsReadFalseOrderByCreatedAtDesc();
    
    // Find notifications by type
    List<AdminNotification> findByTypeOrderByCreatedAtDesc(String type);
    
    // Count unread notifications
    long countByIsReadFalse();
    
    // Find recent notifications (last 24 hours)
    @Query("SELECT n FROM AdminNotification n WHERE n.createdAt >= :since ORDER BY n.createdAt DESC")
    List<AdminNotification> findRecentNotifications(@Param("since") java.time.LocalDateTime since);
    
    // Find notifications for specific order
    List<AdminNotification> findByOrderIdOrderByCreatedAtDesc(Long orderId);
} 