package com.momnect.productservice.command.entity.area;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductTradeAreaId implements Serializable {
    private Long productId;
    private Integer areaId;
}
