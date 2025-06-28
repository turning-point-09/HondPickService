package com.example.handPick.dto;

// Lombok annotations for boilerplate code
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter; // Not strictly needed if @Data is used, but harmless.
import lombok.Getter; // Not strictly needed if @Data is used, but harmless.

@Getter // Redundant if @Data is used, but harmless.
@Setter // Redundant if @Data is used, but harmless.
@Data // Generates getters, setters, toString, equals, and hashCode. This is sufficient.
@NoArgsConstructor // Generates a no-argument constructor
@AllArgsConstructor // Generates a constructor with all fields
public class ProductDto {
    private Long id;
    private String name;
    private String description;
    private Double price; // Changed to Double for consistency with nullable in entity/service
    private String imageUrl; // For displaying product images in frontend
    private int stockQuantity; // To check if product is available

    // --- NEW FIELDS TO ADD / UPDATE ---
    private Double rating; // Add this field, use Double for nullable decimal
    private Double oldPrice; // Add this field, use Double for nullable
    private Double discountPercentage; // Ensure this is also Double for consistency and nullability

    // No manual getters/setters/constructors needed due to Lombok annotations
}