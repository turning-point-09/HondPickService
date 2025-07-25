package com.example.handPick.repository;

import com.example.handPick.model.Review;
import com.example.handPick.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProduct(Product product);
}