package com.momnect.postservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        /*  ✅ 개발용 테스트를 위해 헤더 인증 비활성화
        String userId = request.getHeader("X-User-Id");
        String role = request.getHeader("X-User-Role");

        log.info("[HeaderAuthenticationFilter] userId : {}", userId);
        log.info("[HeaderAuthenticationFilter] role : {}", role);

        if (userId != null && role != null) {
            PreAuthenticatedAuthenticationToken authentication =
                new PreAuthenticatedAuthenticationToken(userId, null,
                    List.of(new SimpleGrantedAuthority(role)));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        */

        // 그냥 무조건 통과
        filterChain.doFilter(request, response);
    }
}
