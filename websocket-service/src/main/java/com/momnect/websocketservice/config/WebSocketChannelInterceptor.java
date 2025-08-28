package com.momnect.websocketservice.config;

import com.momnect.websocketservice.command.dto.UserValidationRequest;
import com.momnect.websocketservice.command.dto.UserValidationResponse;
import com.momnect.websocketservice.command.feign.UserServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketChannelInterceptor implements ChannelInterceptor {

    private final UserServiceClient userServiceClient;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            log.info("=== WebSocket CONNECT 요청 시작 ===");
            log.info("Session ID: {}", accessor.getSessionId());
            log.info("Headers: {}", accessor.toNativeHeaderMap());
            
            // 클라이언트에서 전송한 사용자 정보 헤더 확인
            String userId = accessor.getFirstNativeHeader("user-id");
            String userName = accessor.getFirstNativeHeader("user-name");
            
            log.info("클라이언트 헤더 - userId: {}, userName: {}", userId, userName);
            
            if (userId != null && !userId.isEmpty()) {
                // 사용자 정보가 있는 경우
                accessor.setUser(() -> userName != null ? userName : "user-" + userId);
                accessor.setSessionAttributes(java.util.Map.of(
                    "userId", userId,
                    "username", userName != null ? userName : "user-" + userId,
                    "roles", List.of("USER")
                ));
                
                log.info("=== WebSocket 연결 허용 (인증된 사용자): {} ({}) ===", userName, userId);
            } else {
                // 사용자 정보가 없는 경우 (익명 사용자)
                String sessionId = accessor.getSessionId();
                accessor.setUser(() -> "anonymous-" + sessionId);
                accessor.setSessionAttributes(java.util.Map.of(
                    "userId", "anonymous-" + sessionId,
                    "username", "Anonymous User",
                    "roles", List.of("ANONYMOUS")
                ));
                
                log.info("=== WebSocket 연결 허용 (익명 사용자): {} ===", sessionId);
            }
        }
        
        return message;
    }
}
