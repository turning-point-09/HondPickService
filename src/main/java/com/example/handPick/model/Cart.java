package com.example.handPick.model;

import jakarta.persistence.*;
import lombok.Data; // Keep @Data for other getters/setters/toString/equalsAndHashCode
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
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
    @JoinColumn(name = "user_id", referencedColumnName = "id", unique = true)
    private User user;

    @Column(unique = true, columnDefinition = "BINARY(16)")
    private UUID guestId;

    // Use EAGER for now to ensure items are loaded when you fetch a cart
    // For production, consider LAZY loading and fetch joins to optimize
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<CartItem> items = new ArrayList<>(); // Initialize to prevent NullPointerException

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CartStatus status = CartStatus.ACTIVE;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // --- Dynamic Calculation Methods (REMOVE THE @Column FIELDS FOR THESE) ---
    // These will now be calculated every time they are accessed

    // Instead of a stored column, calculate total price dynamically
    public BigDecimal getTotalPrice() {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Instead of a stored column, calculate total items dynamically
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
        if (this.guestId == null && this.user == null) {
            this.guestId = UUID.randomUUID();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum CartStatus {
        ACTIVE, ORDERED, ABANDONED
    }
}