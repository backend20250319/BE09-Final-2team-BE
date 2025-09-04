package com.momnect.productservice.command.dto.product;

import com.momnect.productservice.command.entity.product.ProductStatus;
import com.momnect.productservice.command.entity.product.RecommendedAge;
import com.momnect.productservice.command.entity.product.SortOption;
import lombok.Getter;

import java.util.List;

@Getter
public class ProductSearchRequest {
    private String query;                   // 키워드
    private Long categoryId;                // 카테고리 ID
    private Integer priceMin;               // 최소 가격
    private Integer priceMax;               // 최대 가격
    private List<RecommendedAge> ageGroups; // 추천 연령대 enum
    private List<Long> areaIds;             // 지역 ID 리스트 (읍/면/동)
    private Boolean excludeSoldOut;         // 판매완료 제외 여부
    private List<ProductStatus> statuses;   // 상품 상태 리스트 (NEW, USED)
    private SortOption sort;                // 정렬 옵션 enum
    private Integer page;                   // 페이지 번호
    private Integer size;                   // 페이지 크기
}

