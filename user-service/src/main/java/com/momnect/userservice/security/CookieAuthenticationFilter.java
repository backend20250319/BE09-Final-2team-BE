package com.momnect.userservice.security;

import com.momnect.userservice.jwt.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
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
 * 쿠키 기반 JWT 인증 필터
 * UsernamePasswordAuthenticationToken 사용
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CookieAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. 헤더 방식 우선 확인 (Gateway에서 온 요청)
        String userId = request.getHeader("X-User-Id");
        String role = request.getHeader("X-User-Role");

        if (userId != null && role != null) {
            log.debug("[CookieAuthenticationFilter] Header auth - userId: {}, role: {}", userId, role);
            setAuthentication(userId, role);
        } else {
            // 2. 쿠키 방식 확인 (직접 접근)
            String accessToken = extractTokenFromCookie(request, "accessToken");

            if (accessToken != null && jwtTokenProvider.validateToken(accessToken)) {
                Long userIdFromToken = jwtTokenProvider.getUserIdFromToken(accessToken);
                String roleFromToken = jwtTokenProvider.getRoleFromToken(accessToken);

                log.debug("[CookieAuthenticationFilter] Cookie auth - userId: {}, role: {}",
                        userIdFromToken, roleFromToken);

                // 다른 부분에서 사용할 수 있도록 request에 추가
                request.setAttribute("X-User-Id", userIdFromToken.toString());
                request.setAttribute("X-User-Role", roleFromToken);

                setAuthentication(userIdFromToken.toString(), roleFromToken);
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 쿠키에서 토큰 추출
     */
    private String extractTokenFromCookie(@NonNull HttpServletRequest request, @NonNull String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Spring Security 인증 설정
     */
    private void setAuthentication(String userId, String role) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}