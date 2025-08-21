package com.momnect.productservice.command.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_product_image")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(length = 2048, nullable = false)
    private String imageUrl;

    @Column(nullable = false)
    private Boolean isThumbnail;
}

