package com.example.handPick.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "carts")
@Data // Provides getters, setters, equals, hashCode, toString
@NoArgsConstructor
@AllArgsConstructor // Keep for constructor with all fields if needed elsewhere
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    // Using EAGER fetch for items for simplicity in current CartService logic.
    // For large applications, consider LAZY loading and fetch joins to optimize performance
    // (e.g., using @EntityGraph or specific JPQL/Criteria queries).
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<CartItem> items = new ArrayList<>(); // Initialize to prevent NullPointerException

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CartStatus status = CartStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // --- Dynamic Calculation Methods (NO @Column ANNOTATIONS HERE) ---
    // These methods calculate totals on the fly, ensuring consistency with cart items
    // and avoiding database default value issues.

    /**
     * Calculates the total price of all items in the cart.
     * @return The sum of (item price * item quantity) for all cart items.
     */
    public BigDecimal getTotalPrice() {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP); // Ensure consistent rounding for total
    }

    /**
     * Calculates the total number of items (sum of quantities) in the cart.
     * @return The sum of quantities for all cart items.
     */
    public Integer getTotalItems() {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        return items.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Enum for cart status
    public enum CartStatus {
        ACTIVE, ORDERED, ABANDONED
    }
}