package com.momnect.productservice.command.controller;

import com.momnect.productservice.command.dto.product.ProductDTO;
import com.momnect.productservice.command.dto.product.ProductRequest;
import com.momnect.productservice.command.dto.product.ProductSummaryDto;
import com.momnect.productservice.command.service.ProductService;
import com.momnect.productservice.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /***
     * 상품 요약 리스트 조회 API
     * ex) /products/summary?ids=1,2,3
     *
     * @param productIds 조회할 상품 ID 리스트
     * @param userId 요청한 사용자 ID (로그인하지 않은 경우 null)
     * @return 상품 요약 정보 리스트
     */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<List<ProductSummaryDto>>> getProductSummaries(
            @RequestParam List<Long> productIds,
            @AuthenticationPrincipal Long userId) {

        List<ProductSummaryDto> summaries = productService.getProductsByIds(productIds, userId);
        return ResponseEntity.ok(ApiResponse.success(summaries));
    }

    /***
     * 상품 상세 조회 API
     * @param productId 조회할 상품 ID
     * @return ProductDTO
     */
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductDTO>> getProduct(@PathVariable Long productId) {

        ProductDTO productDTO = productService.getProduct(productId);
        return ResponseEntity.ok(ApiResponse.success(productDTO));
    }

    /***
     * 상품 등록 API
     * @param dto 상품 등록 요청 ProductRequest
     * @return 등록된 상품의 ID
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Long>>> createProduct(
            @RequestBody ProductRequest dto,
            @AuthenticationPrincipal String userId) {

        Long productId = productService.createProduct(dto, userId);
        Map<String, Long> result = Map.of("productId", productId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
