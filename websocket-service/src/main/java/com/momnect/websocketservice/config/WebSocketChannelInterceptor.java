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
        
        log.info("=== WebSocket 프레임 처리 시작 ===");
        log.info("STOMP Command: {}", accessor.getCommand());
        log.info("Session ID: {}", accessor.getSessionId());
        
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            log.info("=== WebSocket CONNECT 요청 시작 ===");
            log.info("Session ID: {}", accessor.getSessionId());
            log.info("Headers: {}", accessor.toNativeHeaderMap());
            
            // 클라이언트에서 전송한 사용자 정보 헤더 확인
            String userId = accessor.getFirstNativeHeader("user-id");
            String userName = accessor.getFirstNativeHeader("user-name");
            String authorization = accessor.getFirstNativeHeader("Authorization");
            
            log.info("클라이언트 헤더 - userId: {}, userName: {}, hasAuth: {}", userId, userName, authorization != null);
            log.info("헤더 상세 정보:");
            log.info("  user-id: '{}'", userId);
            log.info("  user-name: '{}'", userName);
            log.info("  Authorization: {}", authorization != null ? "있음" : "없음");
            
            if (userId != null && !userId.isEmpty()) {
                // 사용자 정보가 있는 경우
                accessor.setUser(() -> userName != null ? userName : "user-" + userId);
                
                // 세션 속성에 사용자 정보와 인증 토큰 저장
                java.util.Map<String, Object> sessionAttributes = new java.util.HashMap<>();
                sessionAttributes.put("userId", userId);
                sessionAttributes.put("username", userName != null ? userName : "user-" + userId);
                sessionAttributes.put("roles", List.of("USER"));
                if (authorization != null && !authorization.isEmpty()) {
                    sessionAttributes.put("Authorization", authorization);
                    log.info("Authorization 토큰 세션에 저장: {}", authorization.substring(0, Math.min(20, authorization.length())) + "...");
                }
                
                accessor.setSessionAttributes(sessionAttributes);
                
                log.info("=== WebSocket 연결 허용 (인증된 사용자): {} ({}) ===", userName, userId);
                log.info("세션 속성 저장 완료: {}", sessionAttributes);
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
        } else if (StompCommand.SEND.equals(accessor.getCommand())) {
            // SEND 프레임에서도 Authorization 헤더 확인
            String authorization = accessor.getFirstNativeHeader("Authorization");
            if (authorization != null && !authorization.isEmpty()) {
                // 세션에 토큰 업데이트
                if (accessor.getSessionAttributes() != null) {
                    accessor.getSessionAttributes().put("Authorization", authorization);
                    log.info("SEND 프레임에서 Authorization 토큰 업데이트: {}", authorization.substring(0, Math.min(20, authorization.length())) + "...");
                    
                    // 세션에 사용자 정보가 없는 경우, 토큰에서 추출 시도
                    String sessionUserId = (String) accessor.getSessionAttributes().get("userId");
                    String sessionUsername = (String) accessor.getSessionAttributes().get("username");
                    
                    if (sessionUserId == null || sessionUsername == null) {
                        log.info("세션에 사용자 정보가 없어 토큰에서 추출 시도");
                        
                        // Authorization 헤더에서 user-id, user-name 확인
                        String userId = accessor.getFirstNativeHeader("user-id");
                        String userName = accessor.getFirstNativeHeader("user-name");
                        
                        if (userId != null && !userId.isEmpty()) {
                            accessor.getSessionAttributes().put("userId", userId);
                            accessor.getSessionAttributes().put("username", userName != null ? userName : "user-" + userId);
                            log.info("SEND 프레임에서 사용자 정보 복원: userId={}, username={}", userId, userName);
                        } else {
                            log.warn("SEND 프레임에서도 사용자 정보를 찾을 수 없음");
                        }
                    }
                }
            }
        }
        
        return message;
    }
}
