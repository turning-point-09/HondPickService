package com.example.handPick.model; // Corrected package name to handPick

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal; // For precise currency handling
import java.time.LocalDateTime;

@Entity
@Table(name = "products") // Explicitly define table name
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT") // Use TEXT for potentially long descriptions
    private String description;

    @Column(nullable = false)
    private BigDecimal price; // Use BigDecimal for prices

    // FIX: Ensure stockQuantity is Integer (wrapper class) to allow null values
    // This resolves "Operator '!=' cannot be applied to 'int', 'null'" error
    private Integer stockQuantity; // Can be null if stock is not specified or managed

    private String imageUrl; // Optional image URL

    @Column(scale = 2) // Rating with 2 decimal places
    private Double rating; // Use Double for rating (0-5)

    private BigDecimal oldPrice; // Optional old price for discounts

    private Double discountPercentage; // Optional discount percentage (0-100)

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Lifecycle callbacks to ensure consistency before persisting/updating
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        calculateDiscountFields();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        calculateDiscountFields();
    }

    // Helper method to calculate oldPrice or discountPercentage if one is provided
    // This ensures data consistency for pricing and discounts.
    private void calculateDiscountFields() {
        // If oldPrice is provided but discountPercentage is not, calculate discountPercentage
        if (this.oldPrice != null && this.price != null && this.discountPercentage == null) {
            if (this.oldPrice.compareTo(this.price) > 0) { // oldPrice > price
                BigDecimal discount = this.oldPrice.subtract(this.price);
                this.discountPercentage = discount.divide(this.oldPrice, 4, BigDecimal.ROUND_HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();
                this.discountPercentage = Math.round(this.discountPercentage * 100.0) / 100.0; // Round to 2 decimal places
            } else {
                this.discountPercentage = 0.0; // No discount if current price is higher or equal
            }
        }
        // If discountPercentage is provided but oldPrice is not, calculate oldPrice
        else if (this.discountPercentage != null && this.price != null && this.oldPrice == null) {
            if (this.discountPercentage > 0 && this.discountPercentage <= 100) {
                // oldPrice = price / (1 - discountPercentage / 100)
                BigDecimal multiplier = BigDecimal.ONE.subtract(BigDecimal.valueOf(this.discountPercentage).divide(BigDecimal.valueOf(100), 4, BigDecimal.ROUND_HALF_UP));
                if (multiplier.compareTo(BigDecimal.ZERO) > 0) { // Avoid division by zero
                    this.oldPrice = this.price.divide(multiplier, 2, BigDecimal.ROUND_HALF_UP);
                } else {
                    this.oldPrice = this.price; // Fallback if multiplier is zero/negative
                }
            } else {
                this.oldPrice = this.price; // No old price if discount is invalid or 0
            }
        }
        // If both are provided, you might want to re-evaluate or trust the provided values.
        // For robustness, if both are provided, we will prioritize oldPrice and recalculate discountPercentage
        // to ensure consistency between them.
        else if (this.oldPrice != null && this.price != null && this.discountPercentage != null) {
            if (this.oldPrice.compareTo(this.price) > 0) {
                BigDecimal discount = this.oldPrice.subtract(this.price);
                this.discountPercentage = discount.divide(this.oldPrice, 4, BigDecimal.ROUND_HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();
                this.discountPercentage = Math.round(this.discountPercentage * 100.0) / 100.0; // Round to 2 decimal places
            } else {
                this.discountPercentage = 0.0;
            }
        }
    }
}
