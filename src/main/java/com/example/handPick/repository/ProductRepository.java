package com.example.handPick.repository;

import com.example.handPick.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByName(String name);
    List<Product> findByStockQuantityGreaterThan(int quantity);

    Page<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String name, String desc, Pageable pageable);
    // Search by name OR description (case-insensitive, containing)
    List<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String nameSearchTerm, String descriptionSearchTerm);
}