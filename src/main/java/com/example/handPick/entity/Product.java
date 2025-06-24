package com.example.handPick.entity; // Adjusted package name

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*; // Using Jakarta Persistence API for Spring Boot 3+
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "product") // Maps to the 'product' table in the database
@Data // Lombok annotation to automatically generate getters, setters, toString, equals, and hashCode
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increments the ID
    @Column(name = "id")
    private Long id;

    // Defines a many-to-one relationship with the ProductCategory entity.
    // @JoinColumn specifies the foreign key column in the 'product' table that
    // references the 'product_category' table.
    // 'nullable = false' means a product must always belong to a category.
    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private ProductCategory category;

    @Column(name = "sku")
    private String sku;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "unit_price")
    private BigDecimal unitPrice;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "active")
    private boolean isActive; // Renamed to isActive for clarity (common Java boolean naming convention)

    @Column(name = "units_in_stock")
    private int unitsInStock;

    @Column(name = "date_created")
    @CreationTimestamp // Automatically sets the timestamp when the entity is created
    private Date dateCreated;

    @Column(name = "last_updated")
    @UpdateTimestamp // Automatically updates the timestamp when the entity is modified
    private Date lastUpdated;
}