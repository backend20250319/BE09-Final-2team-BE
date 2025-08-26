package com.momnect.userservice.command.client;

import com.momnect.userservice.command.dto.ProductSummaryDTO;
import com.momnect.userservice.command.dto.TransactionSummaryDTO;
import com.momnect.userservice.common.ApiResponse;
import com.momnect.userservice.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "product-service", url = "http://localhost:8000/api/v1/product-service", configuration = FeignClientConfig.class)
public interface ProductClient {

    // 거래 현황 요약 정보
    @GetMapping("/trades/me/summary")
    ApiResponse<TransactionSummaryDTO> getMyTransactionSummary(@RequestHeader("X-User-Id") Long userId);

    // 구매 상품 목록 조회
    @GetMapping("/trades/me/purchases")
    ApiResponse<List<ProductSummaryDTO>> getMyPurchases(@RequestHeader("X-User-Id") Long userId);

    // 판매 상품 목록 조회
    @GetMapping("/trades/me/sales")
    ApiResponse<List<ProductSummaryDTO>> getMySales(@RequestHeader("X-User-Id") Long userId);
}
