package com.momnect.reviewservice.command.client;

import com.momnect.reviewservice.command.dto.ReviewDTO;
import com.momnect.reviewservice.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Gateway를 통해 접근하는 Product Service의 FeignClient
// name: 유레카 서버에 등록된 Product Service의 이름
// url: 게이트웨이의 주소
@FeignClient(name = "product-service", url = "http://localhost:8761", configuration = FeignClientConfig.class)
public interface ReviewClient {

    // Product Service의 사용자 정보 API 호출
    // 게이트웨이가 /api/v1/product-service/** 경로를 처리한다고 가정
//    @GetMapping("/api/v1/product-service/users/{userId}")
//    ReviewDTO getUserInfo(@PathVariable("userId") Long userId);
}
