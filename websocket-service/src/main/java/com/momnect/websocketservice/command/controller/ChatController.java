package com.momnect.websocketservice.command.controller;

import com.momnect.websocketservice.command.dto.ChatMessageRequest;
import com.momnect.websocketservice.command.dto.ChatMessageResponse;
import com.momnect.websocketservice.command.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.util.UUID;
import com.momnect.websocketservice.command.dto.ReadRequest;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessageRequest chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        log.info("📨 메시지 수신: {}", chatMessage);
        
        // 세션에서 사용자 정보 확인
        String sessionUserId = (String) headerAccessor.getSessionAttributes().get("userId");
        String sessionUsername = (String) headerAccessor.getSessionAttributes().get("username");
        String authorizationToken = (String) headerAccessor.getSessionAttributes().get("Authorization");
        
        log.info("세션 사용자 정보 - userId: {}, username: {}, hasToken: {}", 
                sessionUserId, sessionUsername, authorizationToken != null);
        if (authorizationToken != null) {
            log.info("세션에서 토큰 확인: {}", authorizationToken.substring(0, Math.min(20, authorizationToken.length())) + "...");
        }
        
        try {
            // Chat Service API 호출하여 메시지 저장 (인증 토큰과 함께)
            log.info("💾 DB 저장 시작: roomId={}, senderId={}", chatMessage.getRoomId(), chatMessage.getSenderId());
            ChatMessageResponse savedMessage = chatService.saveMessage(chatMessage, authorizationToken);
            log.info("✅ DB 저장 완료: messageId={}", savedMessage.getId());
            
            // 채팅방의 모든 구독자에게 메시지 브로드캐스트
            String topic = "/topic/room." + chatMessage.getRoomId();
            log.info("📡 브로드캐스트 시작: topic={}", topic);
            messagingTemplate.convertAndSend(topic, savedMessage);
            log.info("✅ 브로드캐스트 완료: topic={}", topic);
            
            // 발신자에게 개인 알림 (선택사항)
            messagingTemplate.convertAndSendToUser(
                chatMessage.getSenderId(), 
                "/queue/notice", 
                "Message sent successfully"
            );
            
        } catch (Exception e) {
            log.error("❌ 메시지 처리 중 오류 발생", e);
            
            // 에러 알림을 발신자에게 전송
            messagingTemplate.convertAndSendToUser(
                chatMessage.getSenderId(), 
                "/queue/error", 
                "Failed to send message: " + e.getMessage()
            );
        }
    }

    @MessageMapping("/chat.addUser")
    public void addUser(@Payload ChatMessageRequest chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        log.info("=== 방 입장 요청 수신 ===");
        log.info("요청 메시지: {}", chatMessage);
        log.info("세션 ID: {}", headerAccessor.getSessionId());
        log.info("세션 속성 전체: {}", headerAccessor.getSessionAttributes());
        
        // 세션에서 사용자 정보 확인
        String sessionUserId = (String) headerAccessor.getSessionAttributes().get("userId");
        String sessionUsername = (String) headerAccessor.getSessionAttributes().get("username");
        String authorizationToken = (String) headerAccessor.getSessionAttributes().get("Authorization");
        
        log.info("방 입장 - 세션 사용자 정보: userId={}, username={}, hasToken={}", 
                sessionUserId, sessionUsername, authorizationToken != null);
        
        if (sessionUserId == null || sessionUsername == null) {
            log.warn("⚠️ 세션에 사용자 정보가 없습니다. WebSocket 연결을 다시 확인해주세요.");
            log.warn("세션 속성 키들: {}", headerAccessor.getSessionAttributes() != null ? 
                    headerAccessor.getSessionAttributes().keySet() : "null");
        }
        
        // 세션에 채팅방 정보 저장
        headerAccessor.getSessionAttributes().put("roomId", chatMessage.getRoomId());
        log.info("✅ 채팅방 정보 세션에 저장: roomId={}", chatMessage.getRoomId());
    }

    @MessageMapping("/chat.markAsRead")
    public void markAsRead(@Payload ReadRequest readRequest, SimpMessageHeaderAccessor headerAccessor) {
        log.info("📖 읽음 처리 요청 수신: 사용자={}, 세션={}", readRequest.getUserId(), headerAccessor.getSessionId());
        
        try {
            // 세션에서 roomId 가져오기
            String roomId = (String) headerAccessor.getSessionAttributes().get("roomId");
            if (roomId == null) {
                log.error("❌ Room ID not found in session for user: {}", readRequest.getUserId());
                return;
            }
            
            log.info("🔍 읽음 처리 시작: roomId={}, userId={}", roomId, readRequest.getUserId());
            
            // Chat Service API 호출하여 읽음 처리
            chatService.markAsRead(roomId, readRequest);
            
            log.info("✅ 읽음 처리 완료: roomId={}, userId={}", roomId, readRequest.getUserId());
            
            // 읽음 처리 완료 알림을 해당 사용자에게 전송
            messagingTemplate.convertAndSendToUser(
                readRequest.getUserId(), 
                "/queue/read-confirmation", 
                "Messages marked as read successfully"
            );
            
            log.info("📤 읽음 처리 완료 알림 전송: userId={}", readRequest.getUserId());
            
        } catch (Exception e) {
            log.error("❌ 읽음 처리 중 오류 발생: userId={}, error={}", readRequest.getUserId(), e.getMessage(), e);
            
            // 에러 알림을 사용자에게 전송
            messagingTemplate.convertAndSendToUser(
                readRequest.getUserId(), 
                "/queue/error", 
                "Failed to mark messages as read: " + e.getMessage()
            );
        }
    }
}
