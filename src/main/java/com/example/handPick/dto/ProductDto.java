package com.example.handPick.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
// @Getter and @Setter are redundant if @Data is used. Removed them for clarity.

@Data // Generates getters, setters, toString, equals, and hashCode. This is sufficient.
@NoArgsConstructor // Generates a no-argument constructor
@AllArgsConstructor // Generates a constructor with all fields
public class ProductDto {
    private Long id;
    private String name;
    private String description;
    private Double price; // Changed to Double for consistency with nullable in entity/service
    private String imageUrl; // For displaying product images in frontend

    // FIX: Changed stockQuantity from int to Integer to correctly handle nullable values
    private Integer stockQuantity; // To check if product is available, now can be null

    // --- NEW FIELDS TO ADD / UPDATE ---
    private Double rating; // Add this field, use Double for nullable decimal
    private Double oldPrice; // Add this field, use Double for nullable
    private Double discountPercentage; // Ensure this is also Double for consistency and nullability

    // No manual getters/setters/constructors needed due to Lombok annotations
}