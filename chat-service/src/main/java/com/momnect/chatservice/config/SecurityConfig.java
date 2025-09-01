package com.momnect.chatservice.config;

import com.momnect.chatservice.security.HeaderAuthenticationFilter;
import com.momnect.chatservice.security.RestAccessDeniedHandler;
import com.momnect.chatservice.security.RestAuthenticationEntryPoint;
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
                .authorizeHttpRequests(auth -> auth
                                // 프리플라이트
                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                                // swagger, auth 공개
                                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**").permitAll()
                                .requestMatchers(HttpMethod.POST, "/auth/signup", "/auth/login").permitAll()

                                // websocket 공개
                                .requestMatchers("/ws/**").permitAll()

                                // ★ 실제 API 전체 공개 (dev 용)
                                // .requestMatchers("/**").permitAll()

                                // 나머지
                                .anyRequest().authenticated()
                                // .anyRequest().permitAll()
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
