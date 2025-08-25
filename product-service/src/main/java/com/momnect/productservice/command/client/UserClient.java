package com.momnect.productservice.command.client;

import com.momnect.productservice.command.client.dto.UserDTO;
import com.momnect.productservice.common.ApiResponse;
import com.momnect.productservice.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// gateway 통해서 접근
@FeignClient(
        name = "user-service-users",
        url = "http://localhost:8000/api/v1/user-service",
        configuration = FeignClientConfig.class)
public interface UserClient {

    /**
     * 타사용자 공개 프로필 정보 조회
     *
     * @param userId 조회할 사용자 ID
     * @return UserDTO
     */
    @GetMapping("users/{userId}")
    ApiResponse<UserDTO> getUserInfo(@PathVariable("userId") Long userId);
}
