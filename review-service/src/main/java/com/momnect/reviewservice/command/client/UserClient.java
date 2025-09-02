package com.momnect.reviewservice.command.client;

import com.momnect.reviewservice.command.client.dto.UserDTO;
import com.momnect.reviewservice.command.dto.ReviewDTO;
import com.momnect.reviewservice.common.ApiResponse;
import com.momnect.reviewservice.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "user-service-users",
        url = "http://localhost:8000/api/v1/user-service",
        configuration = FeignClientConfig.class)
public interface UserClient {

    @GetMapping("/users/{userId}")
    ApiResponse<UserDTO> getUserInfo(@PathVariable("userId") Long userId);

    // FeignClient에서 메서드와 경로 변수를 명확하게 매핑하기 위해 @GetMapping과 @PathVariable을 추가했습니다.
    @GetMapping("/users/{userId}")
    ApiResponse<UserDTO> getUserById(@PathVariable("userId") Long userId);
}
