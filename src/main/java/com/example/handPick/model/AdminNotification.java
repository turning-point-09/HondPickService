package com.example.handPick.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminNotification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false, length = 1000)
    private String message;
    
    @Column(nullable = false)
    private String type; // ORDER_PLACED, ORDER_STATUS_CHANGED, etc.
    
    @Column(nullable = false)
    private boolean isRead = false;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private Long orderId; // Reference to the order
    
    @Column
    private String customerName;
    
    @Column
    private String customerMobile;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
} 