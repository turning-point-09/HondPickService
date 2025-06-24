package com.example.handPick.model; // UPDATED

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "carts")
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

    @Column(unique = true)
    private UUID guestId; // Unique ID for guest carts, nullable for user carts

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    @Column(nullable = false)
    private BigDecimal totalPrice = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CartStatus status = CartStatus.ACTIVE; // ACTIVE, ORDERED, ABANDONED

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (this.guestId == null && this.user == null) {
            this.guestId = UUID.randomUUID(); // Ensure guestId is set if neither user nor guestId is provided
        }
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