package com.example.handPick.repository;

import com.example.handPick.model.Product; // Import the Product entity
import org.springframework.data.jpa.repository.JpaRepository; // Spring Data JPA's core repository interface
import org.springframework.stereotype.Repository; // Marks this as a repository component

import java.util.List;
import java.util.Optional;

@Repository // Marks this interface as a Spring Data JPA repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    // JpaRepository<Entity, PrimaryKeyType> provides:
    // - save(), findById(), findAll(), delete(), etc.

    // You can add custom query methods here if needed, e.g.:
    Optional<Product> findByName(String name);
    List<Product> findByStockQuantityGreaterThan(int quantity);
}