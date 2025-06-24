package com.example.handPick.model;

import jakarta.persistence.Entity; // JPA Entity annotation
import jakarta.persistence.GeneratedValue; // For auto-incrementing ID
import jakarta.persistence.GenerationType; // Strategy for ID generation
import jakarta.persistence.Id; // Primary key annotation
import jakarta.persistence.Table; // To specify table name (optional if class name matches)

// Lombok annotations
import lombok.*;

@Getter
@Setter
@Entity // Marks this class as a JPA entity
@Table(name = "products") // Specifies the table name in the database
@Data // Generates getters, setters, toString, equals, and hashCode
@NoArgsConstructor // Generates a no-argument constructor
@AllArgsConstructor // Generates a constructor with all fields
public class Product {
    @Id // Marks id as the primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increments ID for MySQL/PostgreSQL
    private Long id;

    private String name;
    private String description;
    private double price;
    private String imageUrl; // URL to the product image
    private int stockQuantity;

    // You might add relationships here later, e.g., @OneToMany for OrderItems

    // If not using Lombok, you'd manually add:
    // public Product() {}
    // public Product(Long id, String name, ...) { ... }
    // public Long getId() { ... }
    // public void setId(Long id) { ... }
    // ... all other getters and setters
}