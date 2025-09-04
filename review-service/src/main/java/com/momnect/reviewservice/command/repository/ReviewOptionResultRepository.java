package com.momnect.reviewservice.command.repository;

import com.momnect.reviewservice.command.entity.Review;
import com.momnect.reviewservice.command.entity.ReviewOption;
import com.momnect.reviewservice.command.entity.ReviewOptionResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewOptionResultRepository extends JpaRepository<ReviewOptionResult, Long> {
    List<ReviewOptionResult> findByReview(Review review);
    Optional<ReviewOptionResult> findByReviewAndOption(Review review, ReviewOption option);
    void deleteByReview_ReviewId(Long reviewId);
}