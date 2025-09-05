package com.momnect.productservice.command.client;

import com.momnect.productservice.command.client.dto.ChildDTO;
import com.momnect.productservice.command.client.dto.ReviewCountDTO;
import com.momnect.productservice.command.client.dto.UserDTO;
import com.momnect.productservice.common.ApiResponse;
import com.momnect.productservice.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

// gateway 통해서 접근
@FeignClient(
        name = "review-service",
        configuration = FeignClientConfig.class)
public interface ReviewClient {
    /**
     * 유저가 받은 리뷰 개수 조회
     */
    @GetMapping("/reviews/users/{userId}/count")
    ReviewCountDTO getReceivedReviewCount(@PathVariable("userId") Long userId);
}
