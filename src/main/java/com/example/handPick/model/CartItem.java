package com.example.handPick.model; // UPDATED

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "cart_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    // Assuming a Product entity exists
    private Long productId; // Store product ID directly if Product entity is not fully managed here
    private String productName; // Denormalized for simplicity in cart view
    private BigDecimal unitPrice; // Price at time of adding to cart

    private int quantity;

    @Transient // Not persisted in DB, calculated on the fly
    private BigDecimal subtotal;

    @PostLoad
    @PostPersist
    @PostUpdate
    public void calculateSubtotal() {
        if (unitPrice != null) {
            this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        } else {
            this.subtotal = BigDecimal.ZERO;
        }
    }
}