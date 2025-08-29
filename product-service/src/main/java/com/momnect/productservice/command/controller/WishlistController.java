package com.momnect.productservice.command.controller;

import com.momnect.productservice.command.dto.product.ProductSummaryDto;
import com.momnect.productservice.command.service.WishlistService;
import com.momnect.productservice.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    // 찜하기
    @PostMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> addToWishlist(@PathVariable Long productId,
                                                           @AuthenticationPrincipal String userId) {
        wishlistService.add(productId, Long.valueOf(userId));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 찜취소
    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> removeFromWishlist(@PathVariable Long productId,
                                                   @AuthenticationPrincipal String userId) {
        wishlistService.remove(productId, Long.valueOf(userId));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 내 찜 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductSummaryDto>>> getMyWishlist(
            @AuthenticationPrincipal String userId) {
        List<ProductSummaryDto> wishlist = wishlistService.getMyWishlist(Long.valueOf(userId));
        return ResponseEntity.ok(ApiResponse.success(wishlist));
    }
}
