package com.example.handPick.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @NotBlank(message = "Product name is required")
    private String name;

    @Column(nullable = false, precision = 19, scale = 2)
    @Min(value = 0, message = "Price cannot be negative")
    private BigDecimal price;

    @Column(length = 255)
    private String imageUrl;

    @Column(nullable = false)
    private Integer stockQuantity;

    // Removed precision and scale for Double type, as they are not applicable.
    @Column
    private Double rating;

    // Removed precision and scale for Double type, as they are not applicable.
    @Column
    private Double discountPercentage;

    @Column(precision = 19, scale = 2)
    private BigDecimal oldPrice;

    @Column(length = 255)
    private String sizeOptions;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        calculatePriceAndDiscount();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        calculatePriceAndDiscount();
    }

    // Helper method to ensure price and discount consistency
    private void calculatePriceAndDiscount() {
        if (this.oldPrice != null && this.price != null && this.oldPrice.compareTo(BigDecimal.ZERO) > 0) {
            if (this.price.compareTo(this.oldPrice) < 0) {
                BigDecimal discount = this.oldPrice.subtract(this.price);
                // Ensure scale is set for division result before converting to double
                this.discountPercentage = discount.divide(this.oldPrice, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();
            } else {
                this.discountPercentage = 0.0;
            }
        } else {
            this.oldPrice = null;
            this.discountPercentage = 0.0;
        }
    }
}
