package com.example.handPick.service;

import com.example.handPick.dto.ReviewDto;
import com.example.handPick.dto.ReviewRequestDto;
import com.example.handPick.dto.ReviewReplyDto;
import com.example.handPick.model.Product;
import com.example.handPick.model.Review;
import com.example.handPick.model.User;
import com.example.handPick.repository.ProductRepository;
import com.example.handPick.repository.ReviewRepository;
import com.example.handPick.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private UserRepository userRepository;

    // Submit a review
    @Transactional
    public ReviewDto submitReview(String mobileNumber, ReviewRequestDto dto) {
        User user = userRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Review review = new Review();
        review.setProduct(product);
        review.setUser(user);
        review.setRating(dto.getRating());
        review.setComment(dto.getComment());
        review = reviewRepository.save(review);

        return toDto(review);
    }

    // List reviews for a product
    public List<ReviewDto> getReviewsForProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return reviewRepository.findByProduct(product)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // Admin reply to a review
    @Transactional
    public ReviewDto replyToReview(Long reviewId, ReviewReplyDto replyDto) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));
        review.setAdminReply(replyDto.getReply());
        review = reviewRepository.save(review);
        return toDto(review);
    }

    // Helper to convert entity to DTO
    private ReviewDto toDto(Review review) {
        ReviewDto dto = new ReviewDto();
        dto.setId(review.getId());
        dto.setProductId(review.getProduct().getId());
        dto.setUserId(review.getUser().getId());
        dto.setUserName(review.getUser().getFirstName() + " " + review.getUser().getLastName());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setAdminReply(review.getAdminReply());
        dto.setCreatedAt(review.getCreatedAt());
        return dto;
    }
}