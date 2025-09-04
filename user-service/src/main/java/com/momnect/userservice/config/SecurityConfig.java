package com.momnect.userservice.config;

import com.momnect.userservice.security.JwtAuthenticationFilter;
import com.momnect.userservice.security.RestAccessDeniedHandler;
import com.momnect.userservice.security.RestAuthenticationEntryPoint;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 설정
 * HttpOnly 쿠키 기반 JWT 인증 적용
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final RestAccessDeniedHandler restAccessDeniedHandler;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session
                        -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(
                        exception ->
                                exception.accessDeniedHandler(restAccessDeniedHandler)
                                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                )
                .authorizeHttpRequests(auth -> auth
                        // 회원가입/로그인 관련
                        .requestMatchers(HttpMethod.POST, "/auth/signup", "/auth/login", "/auth/verify-account").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/auth/reset-password").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/validate", "/auth/validate-cookie").permitAll()

                        // API 문서 (모든 HTTP 메서드 허용)
                        .requestMatchers(HttpMethod.GET, "/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**").permitAll()

                        // 공개 사용자 정보
                        .requestMatchers(HttpMethod.GET, "/users/check").permitAll()
                        .requestMatchers(HttpMethod.GET, "/users/search-areas").permitAll()
                        .requestMatchers(HttpMethod.GET, "/users/{userId}/profile-page").permitAll() // 타사용자 프로필 페이지 정보
                        .requestMatchers(HttpMethod.GET, "/users/{userId}/my-trade-locations").permitAll()
                        .requestMatchers(HttpMethod.GET, "/users/{userId}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/users/{userId}/exists").permitAll()
                        .requestMatchers(HttpMethod.GET, "/users/{userId}/basic").permitAll()

                        // 인증 필요
                        .requestMatchers(HttpMethod.POST, "/auth/logout").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/users/me/trade-locations").authenticated()

                        // 나머지는 모두 인증 필요
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        ;

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}