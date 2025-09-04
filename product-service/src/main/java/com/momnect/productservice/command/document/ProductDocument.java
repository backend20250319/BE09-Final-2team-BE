package com.momnect.productservice.command.document;

import com.momnect.productservice.command.entity.product.Product;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDocument {

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

    // LocalDateTime 대신 epoch millis 로 저장
    private Long createdAt;
    private Long updatedAt;
    private Long soldAt;

    private Boolean isDeleted;

    private String emd;                  // 행정동 정보
    private String thumbnailImagePath;   // 대표 이미지 경로
    private List<Integer> tradeAreaIds;     // 거래지역 ids

    public static ProductDocument fromEntity(Product product, String emd, String thumbnailImagePath, List<Integer> tradeAreaIds) {
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
                .createdAt(toMillis(product.getCreatedAt()))
                .updatedAt(toMillis(product.getUpdatedAt()))
                .soldAt(toMillis(product.getSoldAt()))
                .isDeleted(product.getIsDeleted())
                .emd(emd)
                .thumbnailImagePath(thumbnailImagePath)
                .tradeAreaIds(tradeAreaIds)
                .build();
    }

    public static Long toMillis(LocalDateTime time) {
        return time == null ? null :
                time.atZone(ZoneId.systemDefault()) // 서버 타임존 기준 (한국이면 Asia/Seoul)
                        .toInstant()
                        .toEpochMilli();
    }

    public static LocalDateTime toLocalDateTime(Long millis) {
        return millis == null ? null :
                Instant.ofEpochMilli(millis)
                        .atZone(ZoneId.of("Asia/Seoul"))
                        .toLocalDateTime();
    }
}
