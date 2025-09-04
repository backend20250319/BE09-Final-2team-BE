package com.momnect.reviewservice.command.repository;

import com.momnect.reviewservice.command.entity.ReviewSentiment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewSentimentRepository extends JpaRepository<ReviewSentiment, Long> {
    Optional<ReviewSentiment> findByReview_ReviewId(Long reviewId);
    long countBySentiment(String sentiment);
    long countBySentimentAndReview_ReviewId(String sentiment, Long userId);
    List<ReviewSentiment> findBySentiment(String sentiment);

}