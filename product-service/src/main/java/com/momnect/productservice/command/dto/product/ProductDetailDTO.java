package com.momnect.productservice.command.dto.product;

import com.momnect.productservice.command.client.dto.UserDTO;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@Builder
public class ProductDetailDTO {
    private ProductDTO currentProduct; // 현재 조회한 상품 상세
    private UserDTO sellerInfo; // 판매자 정보
    private List<ProductSummaryDto> sellerRecentProducts; // 판매자의 최근 상품 3개
}
