package com.example.handPick.repository;

import com.example.handPick.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByName(String name);
    List<Product> findByStockQuantityGreaterThan(int quantity);

    // New method to search by name OR description (case-insensitive, containing)
    List<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String nameSearchTerm, String descriptionSearchTerm);
}