package com.momnect.chatservice.command.client;

import com.momnect.chatservice.command.client.dto.UserBasicInfoResponse;
import com.momnect.chatservice.command.client.dto.ApiResponse;
import com.momnect.chatservice.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", configuration = FeignClientConfig.class)
public interface UserServiceClient {

    @GetMapping("/users/{userId}/basic")
    ApiResponse<UserBasicInfoResponse> getUserBasicInfo(@PathVariable Long userId);
}
