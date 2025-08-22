package com.momnect.productservice.command.dto;

import lombok.Getter;

@Getter
public class ProductRequest {
    private Long categoryId;
    private Long sellerId;
    private Long buyerId;
    private String name;
    private String content;
    private Integer price;
    private String productStatus;
    private String tradeStatus;
    private String recommendedAge;
    private Integer viewCount = 0;
}
