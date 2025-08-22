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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hashtag_id", nullable = false)
    private Hashtag hashtag;
}

