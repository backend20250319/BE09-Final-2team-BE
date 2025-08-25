package com.momnect.productservice.command.document;

import com.momnect.productservice.command.entity.product.Product;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    private List<String> hashtags;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime soldAt;
    private Boolean isDeleted;

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
                .hashtags(product.getProductHashtags().stream()
                        .map(ph -> ph.getHashtag().getName())
                        .collect(Collectors.toList()))
                .viewCount(product.getViewCount())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .soldAt(product.getSoldAt())
                .isDeleted(product.getIsDeleted()) // default false
                .build();
    }
}
