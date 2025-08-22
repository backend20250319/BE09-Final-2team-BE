package com.momnect.productservice.command.entity.area;

import com.momnect.productservice.command.entity.product.Product;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tbl_product_trade_area")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductTradeArea  {

    @EmbeddedId
    private ProductTradeAreaId id; // product_id + area_id 복합키

    // Product와 ManyToOne 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productId") // 복합키의 productId와 매핑
    @JoinColumn(name = "product_id")
    private Product product;

    // Area와 ManyToOne 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("areaId") // 복합키의 areaId와 매핑
    @JoinColumn(name = "area_id")
    private Area area;
}

