package com.momnect.websocketservice.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignClientConfig {
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {

            /* 현재 요청의 Http Servlet Request 를 가져옴 */
            ServletRequestAttributes requestAttributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (requestAttributes != null) {

                // 1. Authorization 헤더가 있는 경우 (Bearer 토큰)
                /* 현재 요청의 Authorization 헤더 추출 (Bearer 토큰) */
                String authorizationHeader = requestAttributes
                        .getRequest()
                        .getHeader(HttpHeaders.AUTHORIZATION);

                if (authorizationHeader != null) {       // 토큰을 들고 왔다면
                    // Feign client 요청에 "Authorization" 헤더 추가
                    requestTemplate.header(HttpHeaders.AUTHORIZATION, authorizationHeader);
                }

                // 2. 쿠키 기반 인증의 경우
                String cookieHeader = requestAttributes
                        .getRequest()
                        .getHeader("Cookie");

                if (cookieHeader != null) {
                    // Feign client 요청에 "Cookie" 헤더 추가
                    requestTemplate.header("Cookie", cookieHeader);
                }
            }
        };
    }
}
