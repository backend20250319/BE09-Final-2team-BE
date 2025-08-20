package com.momnect.reviewservice.command.controller;

import com.momnect.reviewservice.command.dto.ReviewRequest;
import com.momnect.reviewservice.command.dto.ReviewResponse;
import com.momnect.reviewservice.command.dto.ReviewStatsResponse;
import com.momnect.reviewservice.command.service.ReviewService;
import com.momnect.reviewservice.common.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getAllReviews() {
        List<ReviewResponse> reviews = reviewService.findAllReviews();
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<ReviewStatsResponse>> getReviewStats() {
        // ReviewService의 메소드 이름과 일치하도록 getReviewStats()로 호출
        ReviewStatsResponse stats = reviewService.getReviewStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    // 신규 추가: 종합 리뷰 요약 엔드포인트
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<String>> getReviewSummary(@RequestParam String sentiment) {
        String summary = reviewService.getSentimentSummary(sentiment);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(@RequestBody ReviewRequest reviewRequest) {
        ReviewResponse newReview = reviewService.createReview(reviewRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(newReview));
    }

    @GetMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<ReviewResponse>> getReviewById(@PathVariable Long reviewId) {
        try {
            ReviewResponse review = reviewService.findReviewById(reviewId);
            return ResponseEntity.ok(ApiResponse.success(review));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.failure("REVIEW_NOT_FOUND", e.getMessage()));
        }
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable Long reviewId,
            @RequestBody ReviewRequest reviewRequest) {
        try {
            ReviewResponse updatedReview = reviewService.updateReview(reviewId, reviewRequest);
            return ResponseEntity.ok(ApiResponse.success(updatedReview));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.failure("REVIEW_NOT_FOUND", e.getMessage()));
        }
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable Long reviewId) {
        try {
            reviewService.deleteReview(reviewId);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResponse.success(null));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.failure("REVIEW_NOT_FOUND", e.getMessage()));
        }
    }
}