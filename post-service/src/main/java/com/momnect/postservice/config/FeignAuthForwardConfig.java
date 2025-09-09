package com.momnect.postservice.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignAuthForwardConfig {

    @Bean
    public RequestInterceptor authForwardingInterceptor() {
        return template -> {
            var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return;
            var req = attrs.getRequest();
            var auth = req.getHeader("Authorization");
            if (auth != null && !auth.isBlank()) {
                template.header("Authorization", auth);
            }
            // (선택) 사용자 ID 헤더를 같이 넘기고 싶다면:
            var xUserId = req.getHeader("X-User-Id");
            if (xUserId != null && !xUserId.isBlank()) {
                template.header("X-User-Id", xUserId);
            }
        };
    }
}
