package com.momnect.websocketservice.command.service;

import com.momnect.websocketservice.command.dto.ChatMessageRequest;
import com.momnect.websocketservice.command.dto.ChatMessageResponse;
import com.momnect.websocketservice.command.dto.ReadRequest;
import com.momnect.websocketservice.command.feign.ChatServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatServiceClient chatServiceClient;

    public ChatMessageResponse saveMessage(ChatMessageRequest request, String authorizationToken) {
        log.info("Saving message to chat service: {}", request);
        log.info("Authorization token: {}", authorizationToken != null ? authorizationToken.substring(0, Math.min(20, authorizationToken.length())) + "..." : "null");
        
        try {
            // ChatMessageSendRequest 형태로 변환
            ChatMessageSendRequest sendRequest = new ChatMessageSendRequest();
            sendRequest.setSenderId(Long.parseLong(request.getSenderId()));
            sendRequest.setSenderName(request.getSenderName());
            sendRequest.setMessage(request.getContent());
            
            log.info("Feign 호출 준비: roomId={}, request={}, token={}", 
                    request.getRoomId(), sendRequest, authorizationToken != null ? "있음" : "없음");
            
            // Feign 클라이언트를 사용하여 Chat Service API 호출 (인증 토큰과 함께)
            ApiResponse<ChatMessageResponse> response = chatServiceClient.saveMessage(request.getRoomId(), authorizationToken, sendRequest);
            
            if (response != null && response.isSuccess()) {
                return response.getData();
            } else {
                throw new RuntimeException("Failed to save message: " + (response != null ? response.getMessage() : "Unknown error"));
            }
            
        } catch (Exception e) {
            log.error("Error saving message", e);
            throw new RuntimeException("Failed to save message", e);
        }
    }

    public void markAsRead(String roomId, ReadRequest readRequest) {
        log.info("🔍 ChatService - 읽음 처리 시작: roomId={}, userId={}", roomId, readRequest.getUserId());
        
        try {
            // ChatMessageMarkReadRequest 형태로 변환
            ChatMessageMarkReadRequest markReadRequest = new ChatMessageMarkReadRequest();
            markReadRequest.setUserId(Long.parseLong(readRequest.getUserId()));
            markReadRequest.setUpTo(null); // null이면 현재 시간으로 처리
            
            log.info("📤 Chat Service API 호출 준비: roomId={}, userId={}, request={}", 
                    roomId, readRequest.getUserId(), markReadRequest);
            
            // Feign 클라이언트를 사용하여 Chat Service API 호출
            chatServiceClient.markAsRead(roomId, markReadRequest);
            
            log.info("✅ Chat Service API 호출 성공: roomId={}, userId={}", roomId, readRequest.getUserId());
            
        } catch (Exception e) {
            log.error("❌ Chat Service API 호출 실패: roomId={}, userId={}, error={}", 
                    roomId, readRequest.getUserId(), e.getMessage(), e);
            throw new RuntimeException("Failed to mark messages as read", e);
        }
    }

    // 내부 DTO 클래스들
    public static class ChatMessageSendRequest {
        private Long senderId;
        private String senderName;
        private String message;

        // Getters and Setters
        public Long getSenderId() { return senderId; }
        public void setSenderId(Long senderId) { this.senderId = senderId; }
        public String getSenderName() { return senderName; }
        public void setSenderName(String senderName) { this.senderName = senderName; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class ChatMessageMarkReadRequest {
        private Long userId;
        private String upTo; // LocalDateTime을 String으로 처리

        // Getters and Setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getUpTo() { return upTo; }
        public void setUpTo(String upTo) { this.upTo = upTo; }
    }

    public static class ApiResponse<T> {
        private boolean success;
        private T data;
        private String errorCode;
        private String message;
        private String timestamp;

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public T getData() { return data; }
        public void setData(T data) { this.data = data; }
        public String getErrorCode() { return errorCode; }
        public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }
}
