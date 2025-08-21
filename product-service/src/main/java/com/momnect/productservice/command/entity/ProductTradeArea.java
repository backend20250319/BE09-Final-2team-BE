package com.momnect.productservice.command.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_product_trade_area")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductTradeArea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Product와 ManyToOne 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Area와 ManyToOne 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;
}

