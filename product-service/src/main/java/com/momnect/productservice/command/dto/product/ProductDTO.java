package com.momnect.productservice.command.dto.product;

import com.momnect.productservice.command.dto.image.ProductImageDTO;
import com.momnect.productservice.command.entity.product.Product;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Setter
@Getter
@Builder
public class ProductDTO {

    private Long id;
    private Long categoryId;
    private String categoryName;
    private Long sellerId;
    private Long buyerId;
    private String name;
    private String content;
    private Integer price;
    private String productStatus;
    private String tradeStatus;
    private String recommendedAge;
    private List<ProductImageDTO> images;
    private List<String> hashtags;
    private List<String> tradeAreas;
    private Integer viewCount;
    private Integer wishlistCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime soldAt;
    private LocalDateTime deletedAt;
    private Boolean isDeleted;
    private Boolean inWishlist;
    private Long createdBy;
    private Long updatedBy;

    // Entity -> DTO 변환
    public static ProductDTO fromEntity(
            Product product, List<ProductImageDTO> images, Integer wishlistCount, Boolean inWishlist) {
        return ProductDTO.builder()
                .id(product.getId())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .sellerId(product.getSellerId())
                .buyerId(product.getBuyerId())
                .name(product.getName())
                .content(product.getContent())
                .price(product.getPrice())
                .productStatus(product.getProductStatus().name())
                .tradeStatus(product.getTradeStatus().name())
                .recommendedAge(product.getRecommendedAge().name())
                .images(images)
                .hashtags(
                        product.getProductHashtags()
                                .stream()
                                .map(ph -> ph.getHashtag().getName())
                                .collect(Collectors.toList())
                )
                .tradeAreas(
                        product.getTradeAreas()
                                .stream()
                                .map(pta -> pta.getArea().getName())
                                .collect(Collectors.toList())
                )
                .viewCount(product.getViewCount())
                .wishlistCount(wishlistCount)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .soldAt(product.getSoldAt())
                .deletedAt(product.getDeletedAt())
                .isDeleted(product.getIsDeleted())
                .inWishlist(inWishlist)
                .createdBy(product.getCreatedBy())
                .updatedBy(product.getUpdatedBy())
                .build();
    }
}
