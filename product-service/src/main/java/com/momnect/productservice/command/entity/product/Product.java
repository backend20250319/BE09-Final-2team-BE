package com.momnect.productservice.command.entity.product;

import com.momnect.productservice.command.dto.product.ProductRequest;
import com.momnect.productservice.command.entity.image.ProductImage;
import com.momnect.productservice.command.entity.area.ProductTradeArea;
import com.momnect.productservice.command.entity.hashtag.ProductHashtag;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tbl_product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ProductCategory category;

    @Column(nullable = false)
    private Long sellerId;

    private Long buyerId;

    @Column(length = 20, nullable = false)
    private String name;

    @Column(length = 1500, nullable = false)
    private String content;

    @Column(nullable = false)
    private Integer price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus productStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeStatus tradeStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecommendedAge recommendedAge;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<ProductImage> productImages = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    @Builder.Default
    private List<ProductHashtag> productHashtags = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<ProductTradeArea> tradeAreas = new ArrayList<>();

    @Column(nullable = false)
    private Integer viewCount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime soldAt;

    private LocalDateTime deletedAt;

    @Builder.Default
    private Boolean isDeleted = false;

    @Column(nullable = false)
    private Long createdBy;

    @Column(nullable = false)
    private Long updatedBy;


    // DTO -> Entity 변환
    public static Product fromRequest(ProductRequest dto, ProductCategory category, Long userId) {
        LocalDateTime now = LocalDateTime.now();

        return Product.builder()
                .category(category)
                .sellerId(userId)
                .name(dto.getName())
                .content(dto.getContent())
                .price(dto.getPrice())
                .productStatus(ProductStatus.valueOf(dto.getProductStatus()))
                .tradeStatus(TradeStatus.valueOf(dto.getTradeStatus()))
                .recommendedAge(RecommendedAge.valueOf(dto.getRecommendedAge()))
                .viewCount(dto.getViewCount()) // default 0
                .createdAt(now)
                .updatedAt(now)
                .createdBy(userId)
                .updatedBy(userId)
                .build();
    }
}

