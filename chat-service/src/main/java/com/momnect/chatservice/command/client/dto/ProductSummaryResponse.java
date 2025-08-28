package com.momnect.chatservice.command.client.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSummaryResponse {
    private Long id;
    private Long sellerId;
    private String name;
    private String thumbnailUrl;
    private Boolean inWishlist;
    private Integer price;
    private String emd;
    private LocalDateTime createdAt;
    private String productStatus;
    private String tradeStatus;
    private Boolean isDeleted;
}
