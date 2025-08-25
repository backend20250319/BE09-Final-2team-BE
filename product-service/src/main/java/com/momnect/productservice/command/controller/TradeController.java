package com.momnect.productservice.command.controller;

import com.momnect.productservice.command.dto.product.ProductDetailDTO;
import com.momnect.productservice.command.dto.product.ProductRequest;
import com.momnect.productservice.command.dto.product.ProductSummaryDto;
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
     * 내 거래 현황 요약 조회 (구매수/판매수)
     */
    @GetMapping("/me/summary")
    public ResponseEntity<ApiResponse<TradeSummaryDTO>> getMyTradeSummary(@AuthenticationPrincipal Long userId) {
        TradeSummaryDTO summary = tradeService.getTradeSummary(userId, true);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    /**
     * 특정 유저 거래 현황 요약 조회 (판매수만)
     */
    @GetMapping("/users/{userId}/summary")
    public ResponseEntity<ApiResponse<TradeSummaryDTO>> getUserTradeSummary(@PathVariable Long userId) {
        TradeSummaryDTO summary = tradeService.getTradeSummary(userId, false);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}
