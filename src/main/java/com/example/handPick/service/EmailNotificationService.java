package com.example.handPick.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationService.class);
    
    @Value("${app.notifications.email.enabled:false}")
    private boolean emailEnabled;
    
    @Value("${app.notifications.email.admin:}")
    private String adminEmail;
    
    @Autowired
    private JavaMailSender mailSender;
    
    /**
     * Send email notification to admin (optional - requires Gmail setup)
     */
    public void sendAdminNotification(String subject, String message) {
        if (!emailEnabled || adminEmail.isEmpty()) {
            logger.info("Email notifications disabled or admin email not configured");
            return;
        }
        
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(adminEmail);
            mailMessage.setSubject(subject);
            mailMessage.setText(message);
            
            mailSender.send(mailMessage);
            logger.info("Email notification sent to admin: {}", subject);
            
        } catch (Exception e) {
            logger.error("Failed to send email notification", e);
        }
    }
    
    /**
     * Send new order notification via email
     */
    public void sendNewOrderEmail(String customerName, String customerMobile, 
                                 Long orderId, String totalAmount, String paymentMethod) {
        String subject = "New Order #" + orderId + " - HandPick";
        String message = String.format(
            "🔔 NEW ORDER ALERT!\n\n" +
            "Order ID: %d\n" +
            "Customer: %s (%s)\n" +
            "Amount: ₹%s\n" +
            "Payment: %s\n\n" +
            "Please check your admin dashboard for details.",
            orderId, customerName, customerMobile, totalAmount, paymentMethod
        );
        
        sendAdminNotification(subject, message);
    }
} 