package com.momnect.websocketservice.command.feign;

import com.momnect.websocketservice.command.dto.UserValidationRequest;
import com.momnect.websocketservice.command.dto.UserValidationResponse;
import com.momnect.websocketservice.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service", configuration = FeignClientConfig.class)
public interface UserServiceClient {
    
    @PostMapping("/users/api/auth/validate")
    UserValidationResponse validateToken(@RequestBody UserValidationRequest request);
    
    @PostMapping("/users/api/auth/validate-cookie")
    UserValidationResponse validateTokenFromCookie();
}
