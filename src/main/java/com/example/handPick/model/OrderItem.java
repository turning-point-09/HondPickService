package com.example.handPick.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // Corrected: Removed extra '= Fetch'
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private Long productId; // Snapshot of product ID

    @Column(nullable = false)
    private String productName; // Snapshot of product name

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice; // Snapshot of unit price at time of order

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal; // Calculated: unitPrice * quantity

    // New: Stores the selected size/unit for the product in the order
    @Column(length = 50, nullable = true) // Make nullable as not all products might have sizes
    private String size;
}
