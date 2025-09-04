package com.momnect.productservice.command.dto.image;

import com.momnect.productservice.command.entity.image.ProductImage;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductImageDTO {

    private Long imageFileId;
    private Integer sortOrder;
    private String url;

    public static ProductImageDTO fromEntity(ProductImage image, String url) {
        return ProductImageDTO.builder()
                .imageFileId(image.getId().getImageFileId())
                .sortOrder(image.getSortOrder())
                .url(url)
                .build();
    }
}