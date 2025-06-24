package com.example.handPick.dto;

// Lombok annotations for boilerplate code (optional, but highly recommended)
import lombok.*;

@Getter
@Setter
@Data // Generates getters, setters, toString, equals, and hashCode
@NoArgsConstructor // Generates a no-argument constructor
@AllArgsConstructor // Generates a constructor with all fields
public class ProductDto {
    private Long id;
    private String name;
    private String description;
    private double price;
    private String imageUrl; // For displaying product images in frontend
    private int stockQuantity; // To check if product is available

    // If not using Lombok, you'd manually add:
    // public ProductDto() {}
    // public ProductDto(Long id, String name, ...) { ... }
    // public Long getId() { ... }
    // public void setId(Long id) { ... }
    // ... all other getters and setters
}