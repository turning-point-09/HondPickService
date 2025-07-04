package com.example.handPick.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false, precision = 19, scale = 2) // Precision for currency
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING; // PENDING, COMPLETED, CANCELLED, SHIPPED, DELIVERED

    @Enumerated(EnumType.STRING)
    @Column(nullable = true) // Nullable until payment is selected/processed
    private PaymentMethod paymentMethod; // COD, GPAY, PHONEPE

    @Enumerated(EnumType.STRING)
    @Column(nullable = true) // Nullable until payment is processed
    private PaymentStatus paymentStatus; // PENDING, COMPLETED, FAILED

    @Column(nullable = true, length = 255) // Store transaction ID from payment gateway
    private String transactionId;

    // New: Embeddable ShippingAddress
    @Embedded
    private ShippingAddress shippingAddress;

    private LocalDateTime orderDate;

    @PrePersist
    protected void onCreate() {
        orderDate = LocalDateTime.now();
        // Set initial payment status if COD is chosen, otherwise PENDING
        if (this.paymentMethod == PaymentMethod.COD) {
            this.paymentStatus = PaymentStatus.PENDING; // COD is pending until delivery
        } else if (this.paymentMethod != null) {
            this.paymentStatus = PaymentStatus.PENDING; // For digital payments, it's pending until confirmed
        }
    }

    public enum OrderStatus {
        PENDING, COMPLETED, CANCELLED, SHIPPED, DELIVERED
    }

    public enum PaymentMethod {
        COD, // Cash on Delivery
        GPAY, // Google Pay
        PHONEPE // PhonePe
    }

    public enum PaymentStatus {
        PENDING,
        COMPLETED,
        FAILED,
        REFUNDED // Optional: if you want to track refunds
    }
}
