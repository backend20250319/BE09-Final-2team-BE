package com.momnect.productservice.config;

import com.momnect.productservice.security.HeaderAuthenticationFilter;
import com.momnect.productservice.security.RestAccessDeniedHandler;
import com.momnect.productservice.security.RestAuthenticationEntryPoint;
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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final RestAccessDeniedHandler restAccessDeniedHandler;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;

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
                .authorizeHttpRequests(auth ->
                        auth
                                .requestMatchers(HttpMethod.GET,
                                        "/products/**",            // 상품 상세/요약/검색/섹션 조회
                                        "/trades/users/*/summary", // 상품 거래 횟수
                                        "/trades/users/*/sales",   // 유저 판매 상품 조회

                                        "/swagger-ui/**",
                                        "/v3/api-docs/**",
                                        "/swagger-resources/**",
                                        "/areas/load",
                                        "/areas/search",
                                        "/areas/**",
                                        "/categories/init",
                                        "/categories/tree"
                                )
                                .permitAll()
                                .requestMatchers(HttpMethod.POST,
                                        "/products/search"
                                )
                                .permitAll()
                                .anyRequest()
                                .authenticated()
                )
                .addFilterBefore(headerAuthenticationFilter(),
                        UsernamePasswordAuthenticationFilter.class)
        ;

        return http.build();
    }

    @Bean
    public HeaderAuthenticationFilter headerAuthenticationFilter() {
        return new HeaderAuthenticationFilter();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
