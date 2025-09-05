package com.momnect.chatservice.command.client;

import com.momnect.chatservice.command.client.dto.ProductSummaryResponse;
import com.momnect.chatservice.command.client.dto.ApiResponse;
import com.momnect.chatservice.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

// **** 예시 파일입니다. 추후 수정해서 사용
// gateway 통해서 접근
@FeignClient(name = "product-service", configuration = FeignClientConfig.class)
public interface ProductClient {

      // 상품 요약 정보 요청 (기존 엔드포인트 활용)
    @GetMapping("/products/summary")
    ApiResponse<List<ProductSummaryResponse>> getProductSummaries(@RequestParam("productIds") List<Long> productIds, @RequestParam("userId") Long userId);

    // 테스트용 - 단일 상품 조회
    @GetMapping("/products/{productId}")
    ApiResponse<ProductSummaryResponse> getProduct(@PathVariable("productId") Long productId);

//    // 지역 정보 요청
//    @GetMapping("/products/areas")
//    AreaDTO getAreas(@RequestBody AreaRequest request);
}
