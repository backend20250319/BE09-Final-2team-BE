package com.momnect.reviewservice.command.repository;

import com.momnect.reviewservice.command.entity.ReviewSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewSummaryRepository extends JpaRepository<ReviewSummary, Long> {
    Optional<ReviewSummary> findBySentiment(String sentiment);
    boolean existsBySentiment(String sentiment);
}
