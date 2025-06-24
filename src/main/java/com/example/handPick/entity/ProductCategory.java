package com.example.handPick.entity; // Adjusted package name

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*; // Using Jakarta Persistence API for Spring Boot 3+
import java.util.Set; // Ensure java.util.Set is imported

@Entity
@Table(name = "product_category") // Maps to the 'product_category' table in the database
@Getter
@Setter // Lombok annotations to automatically generate getters and setters
public class ProductCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increments the ID
    @Column(name = "id")
    private Long id;

    @Column(name = "category_name")
    private String categoryName;

    // Defines a one-to-many relationship with the Product entity.
    // CascadeType.ALL means that persistence operations (like persist, merge, remove)
    // applied to ProductCategory will cascade to its associated Product entities.
    // 'mappedBy = "category"' indicates that the 'category' field in the Product entity
    // is the owning side of this relationship.
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "category")
    private Set<Product> products; // Collection of products in this category
}