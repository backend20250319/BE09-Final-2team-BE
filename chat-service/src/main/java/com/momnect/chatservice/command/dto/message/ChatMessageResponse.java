package com.momnect.chatservice.command.dto.message;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ChatMessageResponse {
    private String id;          // Mongo ObjectId hex string
    private Long roomId;        // 채팅방 ID (chatRoomId와 동일하지만 더 명확한 네이밍)
    private Long chatRoomId;    // 기존 필드 (하위 호환성)
    private Long senderId;
    private String message;
    private LocalDateTime sentAt;
    private boolean read;
}
