package com.example.handPick.dto;

import jakarta.persistence.Column;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdateDto {

    @NotNull(message = "Price cannot be null")
    @Min(value = 0, message = "Price cannot be negative")
    private BigDecimal price;

    @NotNull(message = "Stock quantity cannot be null")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    // Optional fields for updating product details
    private String name;
    private String description;
    private String imageUrl;
    private String category;
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    // Add this field if you want to update rating
    private Double rating;

    // Add these if you want to update oldPrice, discountPercentage, sizeOptions, etc.
    private BigDecimal oldPrice;
    private Double discountPercentage;
    private String sizeOptions;

}