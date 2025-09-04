package com.momnect.reviewservice.command.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRankingResponse {
    private Long userId;
    private String nickname; // 사용자 ID 또는 닉네임
    private Long totalReviewCount; // 총 리뷰 개수
    private Double averageRating; // 평균 별점
    private Integer rank; // 순위 (1, 2, 3)
}
