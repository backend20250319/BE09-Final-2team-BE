package com.momnect.postservice.command.client;

import com.momnect.postservice.command.client.dto.PublicUserDTO;
import com.momnect.postservice.common.ApiResponse;
import com.momnect.postservice.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "user-service-users",
        configuration = FeignClientConfig.class)
public interface UserClient {

    @GetMapping("/users/{userId}/basic")
    ResponseEntity<ApiResponse<PublicUserDTO>> getBasicInfo(@PathVariable Long userId);
}
