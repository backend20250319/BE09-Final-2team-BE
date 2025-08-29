package com.momnect.userservice.command.dto.common;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TransactionSummaryDTO {

    private Integer totalSalesCount; //
    private Integer purchaseCount;   // 총 구매 개수
    private Integer salesCount;      // 총 판매 개수
    private Integer reviewCount;     // 작성한 리뷰 개수

    // null 인 경우를 대비한 getter
    public int getTotalSalesCount() {
        return totalSalesCount != null ? totalSalesCount : 0;
    }

    public int getPurchaseCount() {
        return purchaseCount != null ? purchaseCount : 0;
    }

    public int getSalesCount() {
        return salesCount != null ? salesCount : 0;
    }
}