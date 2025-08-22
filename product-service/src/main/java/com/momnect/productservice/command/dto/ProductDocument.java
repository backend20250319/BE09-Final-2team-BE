package com.momnect.productservice.command.dto;

import com.momnect.productservice.command.entity.Product;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@Document(indexName = "products")
public class ProductDocument {

    @Id
    private Long id;

    private Long categoryId;
    private Long sellerId;
    private String name;
    private String content;
    private Integer price;
    private String productStatus;
    private String tradeStatus;
    private String recommendedAge;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime soldAt;

    // Product -> ProductDocument 변환
    public static ProductDocument fromEntity(Product product) {
        return ProductDocument.builder()
                .id(product.getId())
                .categoryId(product.getCategory().getId())
                .sellerId(product.getSellerId())
                .name(product.getName())
                .content(product.getContent())
                .price(product.getPrice())
                .productStatus(product.getProductStatus().name())
                .tradeStatus(product.getTradeStatus().name())
                .recommendedAge(product.getRecommendedAge().name())
                .viewCount(product.getViewCount())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .soldAt(product.getSoldAt())
                .build();
    }
}
