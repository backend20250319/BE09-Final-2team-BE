package com.momnect.productservice.command.controller;

import com.momnect.productservice.command.dto.ProductRequest;
import com.momnect.productservice.command.service.ProductService;
import com.momnect.productservice.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

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
