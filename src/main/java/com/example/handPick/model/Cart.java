package com.example.handPick.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID; // Ensure UUID import for guestId

@Entity
@Table(name = "carts") // Use "carts" table name
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", unique = true)
    private User user; // Nullable for guest carts

    @Column(unique = true, columnDefinition = "BINARY(16)") // Store UUID as BINARY(16) for efficiency
    private UUID guestId; // Unique ID for guest carts, nullable for user carts

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<CartItem> items = new ArrayList<>();

    @Column(nullable = false)
    private BigDecimal totalPrice = BigDecimal.ZERO;

    // FIX: Re-introduced totalItems field
    @Column(nullable = false)
    private Integer totalItems = 0; // Initialize to 0

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CartStatus status = CartStatus.ACTIVE; // ACTIVE, ORDERED, ABANDONED

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // This method calculates totalItems and totalPrice based on the current items in the cart.
    public void updateTotals() {
        this.totalItems = items.stream()
                .mapToInt(CartItem::getQuantity) // Sum the quantities of all items
                .sum();
        this.totalPrice = items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        // Ensure guestId is set if neither user nor guestId is provided
        if (this.guestId == null && this.user == null) {
            this.guestId = UUID.randomUUID();
        }

        // FIX: Call updateTotals() here to ensure initial totals are correct
        updateTotals();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        // FIX: Call updateTotals() here to ensure totals are updated before any save
        updateTotals();
    }

    // Enum for cart status
    public enum CartStatus {
        ACTIVE, ORDERED, ABANDONED
    }

}
