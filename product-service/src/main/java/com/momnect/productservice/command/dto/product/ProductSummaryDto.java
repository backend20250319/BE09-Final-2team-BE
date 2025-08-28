package com.momnect.productservice.command.dto.product;

import com.momnect.productservice.command.document.ProductDocument;
import com.momnect.productservice.command.entity.product.Product;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ProductSummaryDto {

    private Long id;        // 상품 id
    private Long sellerId;  // 판매자 id
    private String name;    // 상품명
    private String thumbnailUrl; // 대표 이미지 URL
    private Boolean inWishlist; // 찜 여부
    private Integer price;
    private String emd; // 거래지역 읍면동
    private LocalDateTime createdAt;
    private String productStatus;
    private String tradeStatus;
    private Boolean isDeleted;

    public static ProductSummaryDto fromEntity(Product product, String thumbnailUrl, Boolean isLiked) {
        return ProductSummaryDto.builder()
                .id(product.getId())
                .sellerId(product.getSellerId())
                .name(product.getName())
                .thumbnailUrl(thumbnailUrl)
                .inWishlist(isLiked)
                .price(product.getPrice())
                .emd(product.getTradeAreas().get(0).getArea().getName())
                .createdAt(product.getCreatedAt())
                .productStatus(product.getProductStatus().name())
                .tradeStatus(product.getTradeStatus().name())
                .isDeleted(product.getIsDeleted())
                .build();
    }
}
