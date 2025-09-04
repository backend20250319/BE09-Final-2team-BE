package com.momnect.productservice.command.entity.hashtag;

import com.momnect.productservice.command.entity.product.Product;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_product_hashtag")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductHashtag {

    @EmbeddedId
    private ProductHashtagId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productId") // 복합키의 productId와 매핑
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("hashtagId") // 복합키의 hashtagId와 매핑
    @JoinColumn(name = "hashtag_id")
    private Hashtag hashtag;
}

