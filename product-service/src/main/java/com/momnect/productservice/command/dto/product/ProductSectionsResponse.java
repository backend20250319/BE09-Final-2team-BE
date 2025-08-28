package com.momnect.productservice.command.dto.product;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ProductSectionsResponse {

    // 인기 상품 섹션
    private List<ProductSummaryDto> popular;

    // 신규 상품 섹션
    private List<ProductSummaryDto> latest;

    // 추천 상품 섹션
    private List<ProductSummaryDto> recommended;
}
