package com.momnect.userservice.command.dto.mypage;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OtherUserTransactionSummaryDTO {

    private int totalSalesCount;    // 총 판매한 상품 개수
    private int salesCount;         // 현재 판매 중인 상품 개수
    private Integer reviewCount;    // 작성한 리뷰 개수
}
