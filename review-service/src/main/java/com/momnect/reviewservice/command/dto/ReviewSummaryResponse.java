package com.momnect.reviewservice.command.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSummaryResponse {
    private String sentiment; // "긍정적" 또는 "부정적"
    private String summaryContent; // AI가 생성한 요약글
    private Long reviewCount; // 해당 감정의 리뷰 개수
    private LocalDateTime updatedAt; // 마지막 업데이트 시간
}
