package com.momnect.reviewservice.command.service;

import com.momnect.reviewservice.command.client.UserClient;
import com.momnect.reviewservice.command.client.ProductClient;
import com.momnect.reviewservice.command.client.dto.UserDTO;
import com.momnect.reviewservice.command.dto.ReviewRequest;
import com.momnect.reviewservice.command.dto.ReviewResponse;
import com.momnect.reviewservice.command.dto.ReviewStatsResponse;
import com.momnect.reviewservice.command.dto.ReviewSummaryResponse;
import com.momnect.reviewservice.command.dto.ReviewDTO;
import com.momnect.reviewservice.command.entity.Review;
import com.momnect.reviewservice.command.entity.ReviewOption;
import com.momnect.reviewservice.command.entity.ReviewOptionResult;
import com.momnect.reviewservice.command.entity.ReviewSentiment;
import com.momnect.reviewservice.command.repository.ReviewRepository;
import com.momnect.reviewservice.command.repository.ReviewOptionRepository;
import com.momnect.reviewservice.command.repository.ReviewOptionResultRepository;
import com.momnect.reviewservice.command.repository.ReviewSentimentRepository;
import com.momnect.reviewservice.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import com.momnect.reviewservice.command.dto.UserRankingResponse;

@Slf4j
@Service
public class ReviewService<ProductDTO> {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReviewOptionRepository reviewOptionRepository;

    @Autowired
    private ReviewOptionResultRepository reviewOptionResultRepository;

    @Autowired
    private ReviewAiService reviewAiService;

    @Autowired
    private ReviewSentimentRepository reviewSentimentRepository;

    @Autowired
    private UserClient userClient;

    @Autowired
    private ProductClient productClient;

    /**
     * 서비스 시작 시 자동으로 모든 감정별 요약글을 생성합니다.
     */
    @PostConstruct
    public void initializeSummaries() {
        try {
            // 서비스 시작 후 잠시 대기 (데이터베이스 연결 대기)
            Thread.sleep(2000);
            regenerateAllSentimentSummaries();
        } catch (Exception e) {
            System.err.println("서비스 시작 시 요약글 초기화 실패: " + e.getMessage());
        }
    }

    // 모든 리뷰 조회 (수정: 감정 정보 포함)
    public List<ReviewResponse> findAllReviews() {
        return reviewRepository.findAll().stream()
                .map(this::convertToDtoWithSentiment)
                .collect(Collectors.toList());
    }
    /**
     * 특정 사용자의 리뷰 목록을 조회합니다.
     * @param userId 조회할 사용자의 ID
     * @return 해당 사용자의 리뷰 목록 (DTO)
     */
    public List<ReviewResponse> getReviewsByUserId(Long userId) {
        return reviewRepository.findByUserId(userId).stream()
                .map(this::convertToDtoWithSentiment)
                .collect(Collectors.toList());
    }
    // 리뷰 통계
    public ReviewStatsResponse getReviewStats() {
        long positiveReviews = reviewSentimentRepository.countBySentiment("긍정적");
        long negativeReviews = reviewSentimentRepository.countBySentiment("부정적");
        long totalReviews = reviewRepository.count();

        double averageRating = reviewRepository.findAll().stream()
                .mapToDouble(Review::getRating)
                .average()
                .orElse(0.0);

        return new ReviewStatsResponse(averageRating, totalReviews, positiveReviews, negativeReviews);
    }

    public ReviewStatsResponse getReviewStats(Long userId) {
        long positiveReviews = reviewSentimentRepository.countBySentimentAndReview_ReviewId("긍정적", userId);
        long negativeReviews = reviewSentimentRepository.countBySentimentAndReview_ReviewId("부정적", userId);
        long totalReviews = reviewRepository.count();

        double averageRating = reviewRepository.findAll().stream()
                .mapToDouble(Review::getRating)
                .average()
                .orElse(0.0);

        return new ReviewStatsResponse(averageRating, totalReviews, positiveReviews, negativeReviews);
    }
    /**
     * 특정 사용자의 총 리뷰 개수를 조회합니다.
     * @param userId 조회할 사용자의 ID
     * @return 해당 사용자의 총 리뷰 개수
     */
    public Integer getReviewCountByUserId(Long userId) {
        return reviewRepository.countByUserId(userId);
    }
    /**
     * 감정별 리뷰 요약글을 가져옵니다. 저장된 요약글이 있으면 반환하고, 없으면 새로 생성합니다.
     */
    public String getSentimentSummary(String sentiment) {
        // 먼저 해당 감정의 리뷰 개수 확인
        long reviewCount = reviewSentimentRepository.countBySentiment(sentiment);

        // 리뷰가 0개인 경우 즉시 반환
        if (reviewCount == 0) {
            return sentiment + " 리뷰가 존재하지 않습니다.";
        }

        // 저장된 요약글을 확인
        String storedSummary = reviewAiService.getStoredSentimentSummary(sentiment);

        // 저장된 요약글이 있고, "존재하지 않습니다"가 아닌 경우 반환
        if (!storedSummary.contains("존재하지 않습니다") && !storedSummary.contains("AI 요약 생성에 실패했습니다")) {
            return storedSummary;
        }

        // 저장된 요약글이 없거나 "존재하지 않습니다"인 경우 새로 생성
        List<ReviewSentiment> filteredSentiments = reviewSentimentRepository.findBySentiment(sentiment);

        if (filteredSentiments.isEmpty()) {
            return sentiment + " 리뷰가 존재하지 않습니다.";
        }

        List<String> contents = filteredSentiments.stream()
                .map(rs -> rs.getReview().getContent())
                .collect(Collectors.toList());

        // AI로 요약글 생성하고 DB에 저장
        String generatedSummary = reviewAiService.generateAndSaveSentimentSummary(sentiment, contents, reviewCount);

        // 생성된 요약글이 실패 메시지인 경우 기본 메시지 반환
        if (generatedSummary.contains("AI 요약 생성에 실패했습니다") || generatedSummary.contains("오류가 발생했습니다")) {
            return sentiment + " 리뷰에 대한 요약을 생성할 수 없습니다.";
        }

        return generatedSummary;
    }

    /**
     * 모든 감정별 요약글을 새로 생성하고 저장합니다.
     */
    @Transactional
    public void regenerateAllSentimentSummaries() {
        // 긍정적 리뷰 요약글 생성
        List<ReviewSentiment> positiveSentiments = reviewSentimentRepository.findBySentiment("긍정적");
        if (!positiveSentiments.isEmpty()) {
            List<String> positiveContents = positiveSentiments.stream()
                    .map(rs -> rs.getReview().getContent())
                    .collect(Collectors.toList());
            reviewAiService.generateAndSaveSentimentSummary("긍정적", positiveContents, (long) positiveContents.size());
        }

        // 부정적 리뷰 요약글 생성
        List<ReviewSentiment> negativeSentiments = reviewSentimentRepository.findBySentiment("부정적");
        if (!negativeSentiments.isEmpty()) {
            List<String> negativeContents = negativeSentiments.stream()
                    .map(rs -> rs.getReview().getContent())
                    .collect(Collectors.toList());
            reviewAiService.generateAndSaveSentimentSummary("부정적", negativeContents, (long) negativeContents.size());
        }
    }

    /**
     * 명예의 전당: 상위 3명 사용자 랭킹을 조회합니다.
     * 총 리뷰 개수와 평균 별점을 기준으로 정렬합니다.
     */
    public List<UserRankingResponse> getTopUsersForHallOfFame() {
        // 1. 모든 리뷰를 가져옵니다.
        List<Review> allReviews = reviewRepository.findAll();

        // 2. userId별로 리뷰를 그룹화하고, 각 사용자의 통계를 계산합니다.
        Map<Long, List<Review>> reviewsByUser = allReviews.stream()
                .collect(Collectors.groupingBy(Review::getUserId));

        List<UserRankingResponse> userStatsList = new ArrayList<>();
        reviewsByUser.forEach((userId, reviews) -> {
            // 별점 평균 계산
            double averageRating = reviews.stream()
                    .mapToDouble(Review::getRating)
                    .average()
                    .orElse(0.0);

            userStatsList.add(new UserRankingResponse(userId, null, (long) reviews.size(), averageRating, null));
        });

        // 3. 별점 평균(내림차순)과 긍정 리뷰 개수(내림차순)를 기준으로 정렬합니다.
        userStatsList.sort(Comparator.comparing(UserRankingResponse::getAverageRating).reversed()
                .thenComparing(ranking -> {
                    // 긍정 리뷰 개수 계산 (별점 4.0 이상)
                    long positiveCount = reviewsByUser.get(ranking.getUserId()).stream()
                            .filter(r -> r.getRating() >= 4.0)
                            .count();
                    return positiveCount;
                }, Comparator.reverseOrder()));

        // 4. 상위 3명을 선택하고 순위를 매깁니다.
        List<UserRankingResponse> top3 = userStatsList.stream()
                .limit(3)
                .collect(Collectors.toList());

        for (int i = 0; i < top3.size(); i++) {
            top3.get(i).setRank(i + 1);
        }

        // 5. UserClient를 통해 사용자 닉네임 정보를 가져옵니다.
        for (UserRankingResponse ranking : top3) {
            try {
                ApiResponse<UserDTO> userResponse = userClient.getUserById(ranking.getUserId());
                if (userResponse.isSuccess() && userResponse.getData() != null) {
                    ranking.setNickname(userResponse.getData().getNickname());
                } else {
                    ranking.setNickname("알 수 없음"); // 닉네임 가져오기 실패 시 기본값 설정
                }
            } catch (RestClientException e) {
                log.error("Failed to fetch user nickname for userId: {}", ranking.getUserId(), e);
                ranking.setNickname("알 수 없음");
            }
        }

        return top3;
    }

    /**
     * 모든 저장된 요약글 정보를 조회합니다.
     */
    public List<ReviewSummaryResponse> getAllSummaries() {
        return reviewAiService.getAllStoredSummaries();
    }

    // 기존 메서드 - ReviewRequest에서 userId와 productId를 가져와서 리뷰 생성
    @Transactional
    public ReviewResponse createReview(ReviewRequest request) {
        // userId와 productId가 null이면 기본값 사용
        Long userId = request.getUserId() != null ? request.getUserId() : 1L;
        Long productId = request.getProductId() != null ? request.getProductId() : 1L;
        return createReview(request, userId, productId);
    }

    // ReviewRequest 외에 userId와 productId를 파라미터로 추가합니다.
    @Transactional
    public ReviewResponse createReview(ReviewRequest request, Long userId, Long productId) {
        // 유저 정보 검증 및 가져오기 (인증 오류 시 무시하고 진행)
        try {
            ApiResponse<UserDTO> userInfo = userClient.getUserInfo(userId);
            log.info("[createReview] ===> userInfo : {}", userInfo.getData());
        } catch (RestClientException e) {
            log.warn("[createReview] 유저 정보 조회 실패 (userId: {}): {}", userId, e.getMessage());
            // 인증 오류가 발생해도 리뷰 생성은 계속 진행
        }

        // 상품 정보 검증 및 가져오기 (인증 오류 시 무시하고 진행)
        try {
            ProductDTO productInfo = (ProductDTO) productClient.getProductInfo(productId);
            log.info("[createReview] ===> productInfo : {}", productInfo);
        } catch (RestClientException e) {
            log.warn("[createReview] 상품 정보 조회 실패 (productId: {}): {}", productId, e.getMessage());
            // 인증 오류가 발생해도 리뷰 생성은 계속 진행
        }
        Map<String, String> analysisResult = reviewAiService.getReviewAnalysis(
                request.getContent(),
                request.getRating(),
                request.getKind(),
                request.getPromise(),
                request.getSatisfaction()
        );

        String summary = analysisResult != null ? analysisResult.get("summary") : null;
        String sentiment = analysisResult != null ? analysisResult.get("sentiment") : null;

        Review review = Review.builder()
                .rating(request.getRating())
                .content(request.getContent())
                .summary(summary)
                .productId(productId)
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Review savedReview = reviewRepository.save(review);
        saveReviewOptions(savedReview, request);

        if (sentiment != null) {
            ReviewSentiment reviewSentiment = ReviewSentiment.builder()
                    .review(savedReview)
                    .sentiment(sentiment)
                    .build();
            reviewSentimentRepository.save(reviewSentiment);

            updateSentimentSummaryAfterReviewChange(sentiment);
        }

        return convertToDtoWithSentiment(savedReview);
    }

    // 리뷰 조회 (id)
    public ReviewResponse findReviewById(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NoSuchElementException("Review not found with id: " + reviewId));
        return convertToDtoWithSentiment(review);
    }

    @Transactional
    public ReviewResponse updateReview(Long reviewId, ReviewRequest request) {
        Review existingReview = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NoSuchElementException("Review not found with id: " + reviewId));

        Map<String, String> analysisResult = reviewAiService.getReviewAnalysis(
                request.getContent(),
                request.getRating(),
                request.getKind(),
                request.getPromise(),
                request.getSatisfaction()
        );

        String summary = analysisResult != null ? analysisResult.get("summary") : null;
        String sentiment = analysisResult != null ? analysisResult.get("sentiment") : null;

        // 새롭게 추가된 내용이 기존 내용과 동일하면 유지 또는 제거
        // 여기서는 기존 내용을 유지하는 것으로 처리합니다.
        if (request.getContent() != null && !request.getContent().equals(existingReview.getContent())) {
            existingReview.setContent(request.getContent());
        }
        if (request.getRating() != existingReview.getRating()) {
            existingReview.setRating(request.getRating());
        }
        if (summary != null) {
            existingReview.setSummary(summary);
        }
        existingReview.setUpdatedAt(LocalDateTime.now());

        Review updatedReview = reviewRepository.save(existingReview);
        updateReviewOptions(updatedReview, request);

        if (sentiment != null) {
            ReviewSentiment reviewSentiment = reviewSentimentRepository.findByReview_ReviewId(updatedReview.getReviewId())
                    .orElseGet(() -> ReviewSentiment.builder().review(updatedReview).build());
            reviewSentiment.setSentiment(sentiment);
            reviewSentimentRepository.save(reviewSentiment);

            // 리뷰 업데이트 후 해당 감정의 요약글 업데이트
            updateSentimentSummaryAfterReviewChange(sentiment);
        }

        return convertToDtoWithSentiment(updatedReview);
    }

    /**
     * 리뷰 변경 후 해당 감정의 요약글을 업데이트합니다.
     */
    private void updateSentimentSummaryAfterReviewChange(String sentiment) {
        // 리뷰 변경이 있을 때마다 모든 감정의 요약글을 재생성
        // 이는 리뷰의 감정이 변경될 수 있기 때문입니다.
        regenerateAllSentimentSummaries();
    }

    // DTO 변환 (감정 정보 포함)
    private ReviewResponse convertToDtoWithSentiment(Review review) {
        ReviewRequest options = getReviewOptions(review);

        String sentiment = reviewSentimentRepository.findByReview_ReviewId(review.getReviewId())
                .map(ReviewSentiment::getSentiment)
                .orElse(null);

        return new ReviewResponse(
                review.getReviewId(),
                review.getRating(),
                review.getContent(),
                review.getSummary(),
                options.getKind() != null ? options.getKind() : false,
                options.getPromise() != null ? options.getPromise() : false,
                options.getSatisfaction() != null ? options.getSatisfaction() : false,
                review.getCreatedAt(),
                review.getUpdatedAt(),
                sentiment
        );
    }

    // 리뷰 옵션 저장
    private void saveReviewOptions(Review review, ReviewRequest request) {
        Map<String, Boolean> optionsMap = new HashMap<>();
        optionsMap.put("KIND", request.getKind());
        optionsMap.put("PROMISE", request.getPromise());
        optionsMap.put("SATISFACTION", request.getSatisfaction());

        optionsMap.forEach((name, flag) -> {
            if (flag != null) {
                ReviewOption option = reviewOptionRepository.findByName(name)
                        .orElseGet(() -> reviewOptionRepository.save(ReviewOption.builder()
                                .name(name)
                                .createdAt(LocalDateTime.now())
                                .createdBy(review.getUserId())
                                .updatedBy(review.getUserId())
                                .build()));

                ReviewOptionResult result = ReviewOptionResult.builder()
                        .review(review)
                        .option(option)
                        .flag(flag)
                        .build();
                reviewOptionResultRepository.save(result);
            }
        });
    }

    // 리뷰 옵션 조회
    private ReviewRequest getReviewOptions(Review review) {
        List<ReviewOptionResult> results = reviewOptionResultRepository.findByReview(review);
        boolean kind = false;
        boolean promise = false;
        boolean satisfaction = false;

        for (ReviewOptionResult result : results) {
            String optionName = result.getOption().getName();
            if ("KIND".equals(optionName)) kind = result.getFlag();
            else if ("PROMISE".equals(optionName)) promise = result.getFlag();
            else if ("SATISFACTION".equals(optionName)) satisfaction = result.getFlag();
        }
        ReviewRequest request = new ReviewRequest();
        request.setRating(review.getRating());
        request.setContent(review.getContent());
        request.setKind(kind);
        request.setPromise(promise);
        request.setSatisfaction(satisfaction);
        return request;
    }

    // 리뷰 옵션 업데이트
    private void updateReviewOptions(Review review, ReviewRequest request) {
        Map<String, Boolean> optionsMap = new HashMap<>();
        optionsMap.put("KIND", request.getKind());
        optionsMap.put("PROMISE", request.getPromise());
        optionsMap.put("SATISFACTION", request.getSatisfaction());

        optionsMap.forEach((name, flag) -> {
            if (flag != null) {
                ReviewOption option = reviewOptionRepository.findByName(name).orElseThrow();
                ReviewOptionResult result = reviewOptionResultRepository.findByReviewAndOption(review, option).orElseThrow();
                result.setFlag(flag);
                reviewOptionResultRepository.save(result);
            }
        });
    }

    /**
     * 리뷰를 삭제합니다.
     */
    @Transactional
    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NoSuchElementException("Review not found with id: " + reviewId));

        // 감정 정보 삭제
        reviewSentimentRepository.findByReview_ReviewId(reviewId)
                .ifPresent(reviewSentimentRepository::delete);

        // 옵션 결과 삭제
        reviewOptionResultRepository.deleteByReview_ReviewId(reviewId);

        // 리뷰 삭제
        reviewRepository.deleteById(reviewId);

        // 삭제된 리뷰의 감정이 있었던 경우 해당 감정의 요약글 업데이트
        // (삭제된 리뷰의 감정을 알 수 없으므로 모든 감정의 요약글을 재생성)
        regenerateAllSentimentSummaries();
    }
}