package com.example.handPick.controller;

import com.example.handPick.model.AdminNotification;
import com.example.handPick.service.NotificationService;
import com.example.handPick.service.EmailNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/admin/notifications")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class AdminNotificationController {

    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private EmailNotificationService emailNotificationService;

    /**
     * GET /api/admin/notifications/test-auth
     * Test authentication endpoint
     */
    @GetMapping("/test-auth")
    public ResponseEntity<String> testAuth(@AuthenticationPrincipal UserDetails userDetails, HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            String message = String.format(
                "Authentication successful! User: %s, Auth Header: %s",
                userDetails != null ? userDetails.getUsername() : "null",
                authHeader != null ? "present" : "missing"
            );
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Authentication test failed: " + e.getMessage());
        }
    }

    /**
     * GET /api/admin/notifications
     * Get all unread notifications for admin
     */
    @GetMapping
    public ResponseEntity<List<AdminNotification>> getUnreadNotifications(HttpServletRequest request) {
        try {
            List<AdminNotification> notifications = notificationService.getUnreadNotifications();
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * GET /api/admin/notifications/recent
     * Get recent notifications (last 24 hours)
     */
    @GetMapping("/recent")
    public ResponseEntity<List<AdminNotification>> getRecentNotifications(HttpServletRequest request) {
        try {
            List<AdminNotification> notifications = notificationService.getRecentNotifications();
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * GET /api/admin/notifications/count
     * Get count of unread notifications
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getUnreadCount(HttpServletRequest request) {
        try {
            long count = notificationService.getUnreadCount();
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(0L);
        }
    }

    /**
     * PATCH /api/admin/notifications/{id}/read
     * Mark a specific notification as read
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, HttpServletRequest request) {
        try {
            notificationService.markAsRead(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * PATCH /api/admin/notifications/read-all
     * Mark all notifications as read
     */
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(HttpServletRequest request) {
        try {
            notificationService.markAllAsRead();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * POST /api/admin/notifications/test-email
     * Test email configuration
     */
    @PostMapping("/test-email")
    public ResponseEntity<String> testEmail(HttpServletRequest request) {
        try {
            emailNotificationService.sendAdminNotification(
                "Test Email - HandPick Admin Notifications",
                "This is a test email to verify that the admin notification system is working correctly.\n\n" +
                "If you receive this email, your configuration is successful! 🎉"
            );
            return ResponseEntity.ok("Test email sent successfully!");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to send test email: " + e.getMessage());
        }
    }
} 