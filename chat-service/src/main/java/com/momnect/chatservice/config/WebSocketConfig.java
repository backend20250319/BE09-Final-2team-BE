package com.momnect.chatservice.config;// WebSocketConfig.java
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;


    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 구독 주소(prefix): /topic, /queue
        config.enableSimpleBroker("/topic", "/queue"); // 단일 인스턴스 기준
        // 메시지 발행(prefix): /app
        config.setApplicationDestinationPrefixes("/app");
        // 유저 큐 prefix: /user (기본값)
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 프론트에서 연결할 엔드포인트 (CORS 허용)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS(); // SockJS 사용시
    }
}
