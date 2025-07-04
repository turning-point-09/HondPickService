package com.example.handPick.model;

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
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 19, scale = 2) // Snapshot of price at time of adding to cart
    private BigDecimal price;

    // New: Stores the selected size/unit for the product in the cart
    @Column(length = 50, nullable = true) // Make nullable as not all products might have sizes
    private String size;

    public CartItem(Product product, Integer quantity, Cart cart) {
        this.product = product;
        this.quantity = quantity;
        this.cart = cart;
        this.price = product.getPrice(); // Capture current price of the product
        // Size will be set separately or default to null if not provided
    }

    // Constructor for when size is provided
    public CartItem(Product product, Integer quantity, Cart cart, String size) {
        this.product = product;
        this.quantity = quantity;
        this.cart = cart;
        this.price = product.getPrice(); // Capture current price of the product
        this.size = size;
    }
}