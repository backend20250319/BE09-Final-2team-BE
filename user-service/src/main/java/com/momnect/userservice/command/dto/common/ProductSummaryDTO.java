package com.momnect.userservice.command.dto.common;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ProductSummaryDTO {

    private Long id;
    private String thumbnailUrl;        // 상품 이미지 썸네일
    private Boolean inWishlist;         // 찜하기
    private Integer price;              // 가격
    private String emd;                 // 읍면동
    private LocalDateTime createdAt;    // 생성일시
    private String productStatus;       // 상품 상태
    private String tradeStatus;         // 거래 상태
    private Boolean isDeleted;          // 삭제 여부
}
