package com.example.handPick.model; // Corrected package name to handPick

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal; // Use BigDecimal for currency

@Entity
@Table(name = "cart_items") // Explicitly define table name
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart; // Reference to the Cart entity

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product; // Reference to the Product entity

    @Column(nullable = false)
    private BigDecimal price; // This stores the price of the product at the time it was added to the cart.

    @Column(nullable = false)
    private Integer quantity; // Use Integer for quantity

    // Constructor for adding new item to cart
    // This constructor is used by CartService when creating a new CartItem
    public CartItem(Product product, Integer quantity, Cart cart) {
        this.product = product;
        this.quantity = quantity;
        // FIX: Directly assign BigDecimal price from Product entity
        this.price = product.getPrice(); // Capture current product price from the Product entity
        this.cart = cart;
    }
}
