package com.example.handPick.controller;

import com.example.handPick.dto.ReviewDto;
import com.example.handPick.dto.ReviewRequestDto;
import com.example.handPick.dto.ReviewReplyDto;
import com.example.handPick.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    // User submits a review
    @PostMapping
    public ResponseEntity<ReviewDto> submitReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ReviewRequestDto dto) {
        ReviewDto review = reviewService.submitReview(userDetails.getUsername(), dto);
        return ResponseEntity.ok(review);
    }

    // List reviews for a product
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ReviewDto>> getReviewsForProduct(@PathVariable Long productId) {
        List<ReviewDto> reviews = reviewService.getReviewsForProduct(productId);
        return ResponseEntity.ok(reviews);
    }

    // Admin replies to a review
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/reply/{reviewId}")
    public ResponseEntity<ReviewDto> replyToReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewReplyDto replyDto) {
        ReviewDto review = reviewService.replyToReview(reviewId, replyDto);
        return ResponseEntity.ok(review);
    }
}