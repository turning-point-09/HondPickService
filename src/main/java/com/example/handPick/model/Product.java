package com.example.handPick.model;

import jakarta.persistence.Entity; // JPA Entity annotation
import jakarta.persistence.GeneratedValue; // For auto-incrementing ID
import jakarta.persistence.GenerationType; // Strategy for ID generation
import jakarta.persistence.Id; // Primary key annotation
import jakarta.persistence.Table; // To specify table name (optional if class name matches)
import jakarta.persistence.Column; // To specify column details like length (optional for basic types)

// Lombok annotations
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Generates getters, setters, toString, equals, and hashCode
@NoArgsConstructor // Generates a no-argument constructor
@AllArgsConstructor // Generates a constructor with all fields
@Entity // Marks this class as a JPA entity
@Table(name = "products") // Specifies the table name in the database
public class Product {
    @Id // Marks id as the primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increments ID for MySQL/PostgreSQL
    private Long id;

    @Column(nullable = false) // Added for non-null constraint
    private String name;

    @Column(length = 1000) // Example for longer description
    private String description;

    @Column(nullable = false) // Added for non-null constraint
    private double price;

    private String imageUrl; // URL to the product image

    @Column(nullable = false) // Added for non-null constraint
    private int stockQuantity;

    // --- NEW FIELDS ADDED BELOW ---

    private Double rating; // New: Optional, use Double for decimal ratings (can be null in DB)

    private Double oldPrice; // New: Optional (can be null in DB)

    private Double discountPercentage; // New: Optional (can be null in DB)

    // Note: If you want to explicitly disallow nulls for these new fields,
    // you would add @Column(nullable = false) above them.
    // However, since they are optional, leaving it off means they can be null.
}