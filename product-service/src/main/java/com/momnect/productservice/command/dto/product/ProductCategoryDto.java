package com.momnect.productservice.command.dto.product;

import com.momnect.productservice.command.entity.product.ProductCategory;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class ProductCategoryDto {
    private Long id;
    private String name;
    private List<ProductCategoryDto> children;

    // 엔티티 -> DTO 변환 (재귀)
    public static ProductCategoryDto fromEntity(ProductCategory category) {
        return ProductCategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .children(category.getChildren() != null
                        ? category.getChildren()
                        .stream()
                        .map(ProductCategoryDto::fromEntity) // 재귀 호출
                        .collect(Collectors.toList())
                        : List.of())
                .build();
    }
}
