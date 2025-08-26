package com.momnect.userservice.command.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class TransactionSummaryDTO {

    private int totalSalesCount; // 총 판매 수
    private int purchaseCount;   // 총 구매 수
    private int reviewCount;     // 작성한 리뷰 개수
}