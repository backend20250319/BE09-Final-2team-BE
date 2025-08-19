package com.momnect.gatewayservice.filter;

import com.momnect.gatewayservice.jwt.GatewayJwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final GatewayJwtTokenProvider jwtTokenProvider;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().toString();

        // 인증 제외 경로들
        if (isExcludedPath(path)) {
            log.debug("🔓 인증 제외 경로: {}", path);
            return chain.filter(exchange);
        }

        String token = null;

        // Authorization 헤더에서 토큰 확인 (기존 방식)
        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            log.debug("📤 헤더에서 토큰 추출: Bearer {}", token.substring(0, Math.min(20, token.length())) + "...");
        } else {
            // 2️⃣ 쿠키에서 토큰 확인 (HttpOnly 쿠키 지원)
            token = extractTokenFromCookie(exchange, "accessToken");
            if (token != null) {
                log.debug("🍪 쿠키에서 토큰 추출: {}", token.substring(0, Math.min(20, token.length())) + "...");
            }
        }

        if (token == null) {
            log.warn("❌ 토큰 없음 - 경로: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        if (!jwtTokenProvider.validateToken(token)) {
            log.warn("❌ 유효하지 않은 토큰 - 경로: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        Long userId = jwtTokenProvider.getUserIdFromJWT(token);
        String role = jwtTokenProvider.getRoleFromJWT(token);

        log.info("✅ 인증 성공 - userId: {}, role: {}, path: {}", userId, role, path);

        ServerHttpRequest mutateRequest = exchange.getRequest()
                .mutate()
                .header("X-User-Id", String.valueOf(userId))
                .header("X-User-Role", role)
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutateRequest)
                .build();

        return chain.filter(mutatedExchange);
    }

    /**
     * 쿠키에서 토큰 추출 (HttpOnly 쿠키 지원)
     */
    private String extractTokenFromCookie(ServerWebExchange exchange, String cookieName) {
        if (exchange.getRequest().getCookies().containsKey(cookieName)) {
            String cookieValue = exchange.getRequest().getCookies().getFirst(cookieName).getValue();
            log.debug("🍪 쿠키 [{}] 값 추출: {}", cookieName, cookieValue.substring(0, Math.min(20, cookieValue.length())) + "...");
            return cookieValue;
        }
        log.debug("🍪 쿠키 [{}] 없음", cookieName);
        return null;
    }

    /**
     * 인증 제외 경로 판단
     */
    private boolean isExcludedPath(String path) {
        return path.startsWith("/api/v1/user-service/auth/signup") ||
                path.startsWith("/api/v1/user-service/auth/login") ||
                path.startsWith("/api/v1/user-service/auth/refresh") ||
                path.contains("/swagger-ui") ||
                path.contains("/v3/api-docs") ||
                path.contains("/actuator");
    }

    @Override
    public int getOrder() {
        return -1;
    }
}