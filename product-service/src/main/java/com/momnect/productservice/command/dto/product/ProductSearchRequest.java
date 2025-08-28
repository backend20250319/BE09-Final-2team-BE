package com.momnect.productservice.command.dto.product;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
public class ProductSearchRequest {
    private String query;              // 키워드
    private Long categoryId;           // 카테고리 ID
    private Integer minPrice;          // 최소 가격
    private Integer maxPrice;          // 최대 가격
    private List<String> recommendedAges; // 추천 연령대
    private List<Long> areaIds;        // 지역 ID 리스트
    private Boolean excludeSold;       // 판매완료 제외 여부
    private Boolean isNew;             // 새상품만
    private Boolean isUsed;            // 중고만
    private String sort;               // 정렬: latest / popular / price_asc / price_desc / relevance
    private Integer page;              // 페이지 번호
    private Integer size;              // 페이지 크기
}

