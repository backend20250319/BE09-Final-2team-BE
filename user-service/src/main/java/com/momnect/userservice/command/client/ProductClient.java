package com.momnect.userservice.command.client;

import com.momnect.userservice.command.dto.common.LocationSearchDTO;
import com.momnect.userservice.command.dto.common.ProductSummaryDTO;
import com.momnect.userservice.command.dto.common.TradeLocationDTO;
import com.momnect.userservice.command.dto.common.TransactionSummaryDTO;
import com.momnect.userservice.common.ApiResponse;
import com.momnect.userservice.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "product-service", configuration = FeignClientConfig.class)
public interface ProductClient {

    // 거래지역 검색 API
    @GetMapping("/areas/search")
    ApiResponse<List<LocationSearchDTO>> searchAreas(@RequestParam String emd);

    // 선택된 거래지역 리스트 조회 API
    @GetMapping("/areas")
    ApiResponse<List<TradeLocationDTO>> getAreasByIds(@RequestParam("areaIds") String areaIds);

    // 거래 현황 요약 정보 (로그인 사용자)
    @GetMapping("/trades/me/summary")
    ApiResponse<TransactionSummaryDTO> getMyTransactionSummary();

    // 구매 상품 목록 조회 (로그인 사용자)
    @GetMapping("/trades/me/purchases")
    ApiResponse<List<ProductSummaryDTO>> getMyPurchases();

    // 판매 상품 목록 조회 (로그인 사용자)
    @GetMapping("/trades/me/sales")
    ApiResponse<List<ProductSummaryDTO>> getMySales();

    // 타 사용자 거래 현황 요약 정보
    @GetMapping("/trades/users/{userId}/summary")
    ApiResponse<TransactionSummaryDTO> getOtherUserTransactionSummary(@PathVariable("userId") Long userId);

    // 타 사용자 판매 상품 목록 조회
    @GetMapping("/trades/users/{sellerId}/sales")
    ApiResponse<List<ProductSummaryDTO>> getOtherUserSales(@PathVariable("sellerId") Long sellerId);

}
