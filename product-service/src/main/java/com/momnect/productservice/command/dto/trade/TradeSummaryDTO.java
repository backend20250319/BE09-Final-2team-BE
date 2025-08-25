package com.momnect.productservice.command.dto.trade;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TradeSummaryDTO {
    private Integer purchaseCount;  // 구매 횟수
    private Integer salesCount;     // 판매 횟수
}