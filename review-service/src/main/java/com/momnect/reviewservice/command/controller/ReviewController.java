package com.momnect.reviewservice.command.controller;

import com.momnect.reviewservice.command.dto.*;
import com.momnect.reviewservice.command.service.ReviewService;
import com.momnect.reviewservice.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    // 특정 사용자의 리뷰 총 개수를 조회하는 기존 엔드포인트
    @GetMapping("/users/{userId}/count")
    public ResponseEntity<ReviewCountResponse> getReviewCountByUserId(@PathVariable Long userId) {
        long count = reviewService.getReviewCountByUserId(userId);
        // long 타입의 count를 Integer로 변환하여 DTO에 전달
        ReviewCountResponse response = new ReviewCountResponse(Math.toIntExact(count));
        return ResponseEntity.ok(response);
    }
    // 신규 추가: 본인이 작성한 리뷰 총 개수 조회
    // Principal 객체를 사용하여 현재 로그인한 사용자의 정보를 자동으로 가져옵니다.
    @GetMapping("/my/count")
    public ResponseEntity<ReviewCountResponse> getMyReviewCount(@AuthenticationPrincipal String username) {
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            Long userId = Long.parseLong(username);
            long count = reviewService.getReviewCountByUserId(userId);
            ReviewCountResponse response = new ReviewCountResponse(Math.toIntExact(count));
            return ResponseEntity.ok(response);
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ReviewCountResponse(0)); // Or a more detailed error message
        }
    }
    // 특정 사용자의 전체 리뷰 내역을 조회하는 신규 엔드포인트
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getReviewsByUserId(@PathVariable Long userId) {
        List<ReviewResponse> reviews = reviewService.getReviewsByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getAllReviews() {
        List<ReviewResponse> reviews = reviewService.findAllReviews();
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    @GetMapping("/users/{userId}/status")
    public ResponseEntity<ApiResponse<ReviewStatsResponse>> getReviewStats(@PathVariable Long userId) {
        ReviewStatsResponse stats = reviewService.getReviewStats(userId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    // 신규 추가: 종합 리뷰 요약 엔드포인트
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<String>> getReviewSummary(@RequestParam String sentiment) {
        String summary = reviewService.getSentimentSummary(sentiment);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    // 신규 추가: 모든 감정별 요약글 재생성 엔드포인트
    @PostMapping("/summary/regenerate")
    public ResponseEntity<ApiResponse<String>> regenerateAllSummaries() {
        reviewService.regenerateAllSentimentSummaries();
        return ResponseEntity.ok(ApiResponse.success("모든 감정별 요약글이 성공적으로 재생성되었습니다."));
    }

    // 신규 추가: 모든 요약글 정보 조회 엔드포인트
    @GetMapping("/summary/all")
    public ResponseEntity<ApiResponse<List<ReviewSummaryResponse>>> getAllSummaries() {
        List<ReviewSummaryResponse> summaries = reviewService.getAllSummaries();
        return ResponseEntity.ok(ApiResponse.success(summaries));
    }

    // 신규 추가: 특정 감정의 요약글 강제 재생성 엔드포인트
    @PostMapping("/summary/regenerate/{sentiment}")
    public ResponseEntity<ApiResponse<String>> regenerateSentimentSummary(@PathVariable String sentiment) {
        String summary = reviewService.getSentimentSummary(sentiment);
        return ResponseEntity.ok(ApiResponse.success(sentiment + " 요약글이 재생성되었습니다: " + summary));
    }

    // 신규 추가: 본인이 작성한 모든 리뷰 목록 조회
    // URL 경로에서 userId를 받지 않고, 인증된 사용자의 ID를 사용합니다.
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getMyReviews(@AuthenticationPrincipal String username) {
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            Long userId = Long.parseLong(username);
            List<ReviewResponse> reviews = reviewService.getReviewsByUserId(userId);
            return ResponseEntity.ok(ApiResponse.success(reviews));
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("INVALID_USER_ID", "인증된 사용자 ID가 유효하지 않습니다."));
        }
    }

    // 신규 추가: 명예의 전당 - 상위 3명 사용자 랭킹 조회
    @GetMapping("/top3")
    public ResponseEntity<ApiResponse<List<UserRankingResponse>>> getHallOfFame() {
        List<UserRankingResponse> topUsers = reviewService.getTopUsersForHallOfFame();
        return ResponseEntity.ok(ApiResponse.success(topUsers));
    }

    // 신규 추가: 현재 저장된 요약글 상태 확인 엔드포인트
    @GetMapping("/summary/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSummaryStatus() {
        Map<String, Object> status = new HashMap<>();

        // 각 감정별 리뷰 개수
        long positiveCount = reviewService.getReviewStats().getPositiveReviews();
        long negativeCount = reviewService.getReviewStats().getNegativeReviews();

        // 각 감정별 요약글
        String positiveSummary = reviewService.getSentimentSummary("긍정적");
        String negativeSummary = reviewService.getSentimentSummary("부정적");

        status.put("positiveReviews", positiveCount);
        status.put("negativeReviews", negativeCount);
        status.put("positiveSummary", positiveSummary);
        status.put("negativeSummary", negativeSummary);

        return ResponseEntity.ok(ApiResponse.success(status));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(@RequestBody ReviewRequest reviewRequest) {
        ReviewResponse newReview = reviewService.createReview(reviewRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(newReview));
    }

    // URL 경로에서 userId와 productId를 받는 새로운 엔드포인트
    @PostMapping("/users/{userId}/products/{productId}")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReviewWithPath(
            @PathVariable Long userId,
            @PathVariable Long productId,
            @RequestBody ReviewRequest reviewRequest) {
        try {
            ReviewResponse newReview = reviewService.createReview(reviewRequest, userId, productId);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(newReview));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.failure("INVALID_REQUEST", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("INTERNAL_ERROR", "리뷰 생성 중 오류가 발생했습니다."));
        }
    }



    @GetMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<ReviewResponse>> getReviewById(@PathVariable Long reviewId) {
        try {
            ReviewResponse review = reviewService.findReviewById(reviewId);
            return ResponseEntity.ok(ApiResponse.success(review));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.failure("REVIEW_NOT_FOUND", "Review not found with id: " + reviewId));
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
                    .body(ApiResponse.failure("REVIEW_NOT_FOUND", "Review not found with id: " + reviewId));
        }
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<String>> deleteReview(@PathVariable Long reviewId) {
        try {
            reviewService.deleteReview(reviewId);
            return ResponseEntity.ok(ApiResponse.success("Review deleted successfully"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.failure("REVIEW_NOT_FOUND", "Review not found with id: " + reviewId));
        }
    }



}