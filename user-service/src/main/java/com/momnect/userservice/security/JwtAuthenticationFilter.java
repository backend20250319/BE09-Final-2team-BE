package com.momnect.userservice.security;

import com.momnect.userservice.jwt.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 기반 인증 필터 (Authorization 헤더)
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Authorization 헤더에서 토큰 추출
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String accessToken = authHeader.substring(7);
            if (jwtTokenProvider.validateToken(accessToken)) {
                String userId = String.valueOf(jwtTokenProvider.getUserIdFromToken(accessToken));
                String role = jwtTokenProvider.getRoleFromToken(accessToken);

                log.debug("[AuthenticationFilter] Authenticated user - userId: {}, role: {}", userId, role);

                // Spring Security 인증 설정
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // 다른 부분에서 사용할 수 있도록 request에 추가
                request.setAttribute("X-User-Id", userId);
                request.setAttribute("X-User-Role", role);
            }
        } else {
            log.debug("[AuthenticationFilter] No Authorization header found or invalid format.");
        }

        filterChain.doFilter(request, response);
    }
}