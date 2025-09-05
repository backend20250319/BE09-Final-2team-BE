package com.momnect.userservice.command.client;

import com.momnect.userservice.command.dto.common.ReviewCountResponseDTO;
import com.momnect.userservice.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "review-service", configuration = FeignClientConfig.class)
public interface ReviewClient {

    // 특정 사용자의 리뷰 총 개수 조회
    @GetMapping("/reviews/users/{userId}/count")
    ReviewCountResponseDTO getReviewCountByUserId(@PathVariable("userId") Long userId);
}
