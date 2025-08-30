package com.momnect.productservice.command.dto.product;

import com.momnect.productservice.command.document.ProductDocument;
import com.momnect.productservice.command.entity.product.Product;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

import static com.momnect.productservice.command.document.ProductDocument.toLocalDateTime;

@Getter
@Setter
@Builder
public class ProductSummaryDto {

    private Long id;
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
    private Boolean hasWrittenReview; // 구매 상품일 경우 리뷰 써야되는지 여부

    public static ProductSummaryDto fromDocument(ProductDocument doc) {
        return ProductSummaryDto.builder()
                .id(doc.getId())
                .sellerId(doc.getSellerId())
                .name(doc.getName())
                .thumbnailUrl(null) // ES에는 이미지 URL이 없으므로 null 처리 (필요시 별도 인덱싱)
                .inWishlist(false)  // ES 검색에는 유저별 찜 여부가 없으므로 기본 false
                .price(doc.getPrice())
                .emd(doc.getEmd()) // ES Document에는 지역명 없음 → 나중에 별도 인덱싱/조인 필요
                .createdAt(toLocalDateTime(doc.getCreatedAt()))
                .productStatus(doc.getProductStatus())
                .tradeStatus(doc.getTradeStatus())
                .isDeleted(doc.getIsDeleted())
                .build();
    }

    public static ProductSummaryDto fromEntity(Product product, String thumbnailUrl, Boolean inWishlist) {
        return ProductSummaryDto.builder()
                .id(product.getId())
                .sellerId(product.getSellerId())
                .name(product.getName())
                .thumbnailUrl(thumbnailUrl)
                .inWishlist(inWishlist)
                .price(product.getPrice())
                .emd(product.getTradeAreas().get(0).getArea().getName())
                .createdAt(product.getCreatedAt())
                .productStatus(product.getProductStatus().name())
                .tradeStatus(product.getTradeStatus().name())
                .isDeleted(product.getIsDeleted())
                .hasWrittenReview(false) // TODO 임시
                .build();
    }
}
