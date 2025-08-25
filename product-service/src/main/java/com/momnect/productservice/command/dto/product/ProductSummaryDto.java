package com.momnect.productservice.command.dto.product;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ProductSummaryDto {

    private Long id;
    private String thumbnailUrl; // 대표 이미지 URL
    private Boolean isLiked; // 찜 여부
    private Integer price;
    private String emd; // 거래지역 읍면동
    private LocalDateTime createdAt;
    private String productStatus;
    private String tradeStatus;
    private Boolean isDeleted;
}
