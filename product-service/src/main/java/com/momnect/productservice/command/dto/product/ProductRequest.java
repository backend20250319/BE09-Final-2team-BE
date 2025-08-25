package com.momnect.productservice.command.dto.product;

import lombok.Getter;

import java.util.List;

@Getter
public class ProductRequest {
    private Long categoryId;
    private String name;
    private String content;
    private Integer price;
    private String productStatus;
    private String tradeStatus;
    private String recommendedAge;
    private final Integer viewCount = 0;
    private List<Long> imageFileIds;
    private List<Integer> areaIds;
    private List<String> hashtags;
}
