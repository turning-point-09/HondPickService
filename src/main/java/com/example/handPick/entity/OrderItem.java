package com.example.handPick.entity; // Adjusted package name

import lombok.Data;

import jakarta.persistence.*; // Using Jakarta Persistence API for Spring Boot 3+
import java.math.BigDecimal;

@Entity
@Table(name = "order_item") // Maps to the 'order_item' table in the database
@Data // Lombok annotation to automatically generate getters, setters, toString, equals, and hashCode
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increments the ID
    @Column(name = "id")
    private Long id;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "unit_price")
    private BigDecimal unitPrice;

    @Column(name = "quantity")
    private int quantity;

    @Column(name = "product_id") // Stores the ID of the product from which this order item was created
    private Long productId;

    // Defines a many-to-one relationship with the Order entity.
    // @JoinColumn specifies the foreign key column in the 'order_item' table that
    // references the 'orders' table.
    @ManyToOne
    @JoinColumn(name = "order_id") // Foreign key column
    private Order order; // The Order this item belongs to
}