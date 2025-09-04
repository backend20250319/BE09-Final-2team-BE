package com.momnect.productservice.command.controller;

import com.momnect.productservice.command.dto.product.ProductDetailDTO;
import com.momnect.productservice.command.dto.product.ProductRequest;
import com.momnect.productservice.command.dto.product.ProductSummaryDto;
import com.momnect.productservice.command.dto.trade.CompleteSaleRequest;
import com.momnect.productservice.command.dto.trade.TradeStatusRequest;
import com.momnect.productservice.command.dto.trade.TradeSummaryDTO;
import com.momnect.productservice.command.service.ProductService;
import com.momnect.productservice.command.service.TradeService;
import com.momnect.productservice.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/trades")
@RequiredArgsConstructor
public class TradeController {

    private final TradeService tradeService;

    /**
     * 내 거래 현황 요약 조회 (총 등록한 상품수/구매수/판매수)
     */
    @GetMapping("/me/summary")
    public ResponseEntity<ApiResponse<TradeSummaryDTO>> getMyTradeSummary(@AuthenticationPrincipal String userId) {
        TradeSummaryDTO summary = tradeService.getTradeSummary(Long.valueOf(userId), true);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    /**
     * 특정 유저 거래 현황 요약 조회 (총 등록한 상품수/판매수)
     */
    @GetMapping("/users/{userId}/summary")
    public ResponseEntity<ApiResponse<TradeSummaryDTO>> getUserTradeSummary(@PathVariable Long userId) {
        TradeSummaryDTO summary = tradeService.getTradeSummary(userId, false);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    /**
     * 내 구매 상품 조회
     */
    @GetMapping("/me/purchases")
    public ResponseEntity<ApiResponse<List<ProductSummaryDto>>> getMyPurchases(@AuthenticationPrincipal String userId) {
        List<ProductSummaryDto> purchases = tradeService.getMyPurchases(Long.valueOf(userId));
        return ResponseEntity.ok(ApiResponse.success(purchases));
    }

    /**
     * 내 판매 상품 조회
     */
    @GetMapping("/me/sales")
    public ResponseEntity<ApiResponse<List<ProductSummaryDto>>> getMySales(@AuthenticationPrincipal String userId) {
        List<ProductSummaryDto> sales = tradeService.getMySales(Long.valueOf(userId));
        return ResponseEntity.ok(ApiResponse.success(sales));
    }

    /**
     * 특정 유저 판매 상품 조회
     */
    @GetMapping("/users/{sellerId}/sales")
    public ResponseEntity<ApiResponse<List<ProductSummaryDto>>> getUserSales(
            @AuthenticationPrincipal String userId,
            @PathVariable Long sellerId) {

        // 로그인 안 한 경우 null 처리
        Long loginUserId = null;
        if (userId != null && !userId.equals("anonymousUser")) {
            loginUserId = Long.valueOf(userId);
        }

        List<ProductSummaryDto> sales = tradeService.getUserSales(loginUserId, sellerId);
        return ResponseEntity.ok(ApiResponse.success(sales));
    }

    /**
     * 상품 판매 완료 처리
     */
    @PatchMapping("/{productId}/complete")
    public ResponseEntity<ApiResponse<Void>> completeSale(
            @PathVariable Long productId,
            @RequestBody CompleteSaleRequest request,
            @AuthenticationPrincipal String userId) {

        tradeService.completeSale(productId, Long.valueOf(userId), request.getBuyerId());

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 상품 거래 상태 변경
     */
    @PatchMapping("/{productId}/status")
    public ResponseEntity<ApiResponse<Void>> updateTradeStatus(
            @PathVariable Long productId,
            @RequestBody TradeStatusRequest request,
            @AuthenticationPrincipal String userId) {

        tradeService.updateTradeStatus(productId, Long.valueOf(userId), request.getNewStatus());

        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
