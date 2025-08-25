package com.momnect.productservice.command.entity.image;

import com.momnect.productservice.command.entity.product.Product;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tbl_product_image")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImage {

    @EmbeddedId
    private ProductImageId id; // product_id + image_file_id 복합키

    @Column(nullable = false)
    private Integer sortOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productId") // 복합키의 productId와 매핑
    @JoinColumn(name = "product_id")
    private Product product;
}

