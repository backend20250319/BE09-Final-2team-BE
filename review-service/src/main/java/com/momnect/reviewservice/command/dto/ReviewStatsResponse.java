package com.momnect.reviewservice.command.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewStatsResponse {
    private double averageRating;
    private long totalReviews;
    private long positiveReviews;
    private long negativeReviews;
}
