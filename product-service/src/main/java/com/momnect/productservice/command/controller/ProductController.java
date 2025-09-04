package com.momnect.productservice.command.controller;

import com.momnect.productservice.command.dto.product.*;
import com.momnect.productservice.command.service.ProductService;
import com.momnect.productservice.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // 찜하기
    @PostMapping("/{productId}/wishlist")
    public ResponseEntity<ApiResponse<Void>> addToWishlist(@PathVariable Long productId,
                                                           @AuthenticationPrincipal String userId) {
        productService.addWishlist(productId, Long.valueOf(userId));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 찜취소
    @DeleteMapping("/{productId}/wishlist")
    public ResponseEntity<ApiResponse<Void>> removeFromWishlist(@PathVariable Long productId,
                                                                @AuthenticationPrincipal String userId) {
        productService.removeWishlist(productId, Long.valueOf(userId));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 내 찜 목록 조회
    @GetMapping("/me/wishlist")
    public ResponseEntity<ApiResponse<List<ProductSummaryDto>>> getMyWishlist(
            @AuthenticationPrincipal String userId) {
        List<ProductSummaryDto> wishlist = productService.getMyWishlist(Long.valueOf(userId));
        return ResponseEntity.ok(ApiResponse.success(wishlist));
    }

    /***
     * 유사 상품 추천
     */
    @GetMapping("/similar")
    public ResponseEntity<ApiResponse<List<ProductSummaryDto>>> getSimilarProducts(
            @RequestParam String keyword,
            @AuthenticationPrincipal String userId) throws IOException {
        List<ProductSummaryDto> wishlist = productService.getSimilarProducts(keyword, parseUserId(userId));
        return ResponseEntity.ok(ApiResponse.success(wishlist));
    }


    /***
     * 홈 일괄 섹션 (선택)
     */
    @GetMapping("/sections")
    public ResponseEntity<ApiResponse<ProductSectionsResponse>> getHomeProductSections(
            @AuthenticationPrincipal String userId) {
        ProductSectionsResponse sections = productService.getHomeProductSections(parseUserId(userId));
        return ResponseEntity.ok(ApiResponse.success(sections));
    }

    /***
     * 상품 검색/브라우즈 API (ES 기반)
     * - query 없으면 카테고리 브라우즈
     * - query 있으면 검색
     * - Pageable 기반으로 Page<ProductSummaryDto> 리턴
     */
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<Page<ProductSummaryDto>>> searchProducts(
            @RequestBody ProductSearchRequest request,
            @AuthenticationPrincipal String userId) throws IOException {

        Page<ProductSummaryDto> result = productService.searchProducts(request, parseUserId(userId));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

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
            @AuthenticationPrincipal String userId) {

        List<ProductSummaryDto> summaries = productService.getProductsByIds(productIds, parseUserId(userId));
        return ResponseEntity.ok(ApiResponse.success(summaries));
    }

    /***
     * 상품 상세 조회 API
     * @param productId 조회할 상품 ID
     * @return ProductDTO
     */
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductDetailDTO>> getProduct(
            @PathVariable Long productId,
            @AuthenticationPrincipal String userId) {

        ProductDetailDTO productDetail = productService.getProductDetail(productId, parseUserId(userId));
        return ResponseEntity.ok(ApiResponse.success(productDetail));
    }

    /***
     * 상품 등록 API
     * @param dto 상품 등록 요청 ProductRequest
     * @return 등록된 상품의 ID
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Long>>> createProduct(
            @RequestBody ProductRequest dto,
            @AuthenticationPrincipal String userId) throws IOException {

        Long productId = productService.createProduct(dto, userId);
        Map<String, Long> result = Map.of("productId", productId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * @param principal 스프링 시큐리티 @AuthenticationPrincipal 값 (String)
     * @return Long userId (비로그인 또는 invalid면 null)
     */
    private static final String ANONYMOUS = "anonymousUser";
    public static Long parseUserId(String principal) {
        if (principal == null || ANONYMOUS.equals(principal)) {
            return null;
        }
        try {
            return Long.valueOf(principal);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
