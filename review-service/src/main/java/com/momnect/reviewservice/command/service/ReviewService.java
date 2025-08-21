package com.momnect.reviewservice.command.service;

import com.momnect.reviewservice.command.dto.ReviewRequest;
import com.momnect.reviewservice.command.dto.ReviewResponse;
import com.momnect.reviewservice.command.dto.ReviewStatsResponse;
import com.momnect.reviewservice.command.entity.Review;
import com.momnect.reviewservice.command.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReviewAiService reviewAiService; // OpenAI AI 서비스 주입

    public List<ReviewResponse> findAllReviews() {
        return reviewRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // 리뷰 통계 조회
    public ReviewStatsResponse getReviewStats() { // 메소드 이름 수정
        List<Review> allReviews = reviewRepository.findAll();
        long totalReviews = allReviews.size();
        if (totalReviews == 0) {
            return new ReviewStatsResponse(0.0, 0, 0, 0);
        }

        double totalRating = allReviews.stream()
                .mapToDouble(Review::getRating)
                .sum();
        double averageRating = totalRating / totalReviews;

        long positiveReviews = allReviews.stream()
                .filter(review -> review.getSummary() != null && review.getSummary().contains("긍정적"))
                .count();

        long negativeReviews = totalReviews - positiveReviews;

        return new ReviewStatsResponse(averageRating, totalReviews, positiveReviews, negativeReviews);
    }

    // 새로운 메소드 추가: 긍정/부정 리뷰 종합 요약
    public String getSentimentSummary(String sentiment) {
        List<Review> allReviews = reviewRepository.findAll();

        List<String> filteredContents = allReviews.stream()
                .filter(review -> {
                    if (sentiment.equalsIgnoreCase("positive")) {
                        return review.getSummary() != null && review.getSummary().contains("긍정적");
                    } else if (sentiment.equalsIgnoreCase("negative")) {
                        return review.getSummary() != null && review.getSummary().contains("부정적");
                    }
                    return false;
                })
                .map(Review::getContent)
                .collect(Collectors.toList());

        return reviewAiService.getCombinedSummary(filteredContents);
    }

    @Transactional
    public ReviewResponse createReview(ReviewRequest request) {
        Review review = new Review();
        review.setContent(request.getContent());
        review.setRating(request.getRating());
        review.setKind(request.getKind());
        review.setPromise(request.getPromise());
        review.setSatisfaction(request.getSatisfaction());
        review.setCreatedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());

        String aiSummary = reviewAiService.getSummaryAndSentiment(request.getContent());
        review.setSummary(aiSummary);

        Review savedReview = reviewRepository.save(review);
        return convertToDto(savedReview);
    }

    public ReviewResponse findReviewById(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NoSuchElementException("Review not found with id: " + reviewId));
        return convertToDto(review);
    }

    @Transactional
    public ReviewResponse updateReview(Long reviewId, ReviewRequest request) {
        Review existingReview = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NoSuchElementException("Review not found with id: " + reviewId));

        existingReview.setContent(request.getContent());
        existingReview.setRating(request.getRating());
        existingReview.setKind(request.getKind());
        existingReview.setPromise(request.getPromise());
        existingReview.setSatisfaction(request.getSatisfaction());
        existingReview.setUpdatedAt(LocalDateTime.now());

        String aiSummary = reviewAiService.getSummaryAndSentiment(request.getContent());
        existingReview.setSummary(aiSummary);

        Review updatedReview = reviewRepository.save(existingReview);
        return convertToDto(updatedReview);
    }

    @Transactional
    public void deleteReview(Long reviewId) {
        if (!reviewRepository.existsById(reviewId)) {
            throw new NoSuchElementException("Review not found with id: " + reviewId);
        }
        reviewRepository.deleteById(reviewId);
    }

    private ReviewResponse convertToDto(Review review) {
        return new ReviewResponse(
                review.getReviewId(),
                review.getRating(),
                review.getContent(),
                review.getSummary(),
                review.getKind(),
                review.getPromise(),
                review.getSatisfaction(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}