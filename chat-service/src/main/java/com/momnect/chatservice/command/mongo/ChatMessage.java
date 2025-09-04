package com.momnect.chatservice.command.mongo;

import lombok.*;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Document(collection = "chat_message")
@CompoundIndex(name = "room_sentAt_idx", def = "{'room_id': 1, 'sent_at': -1}")
public class ChatMessage {

    @Id
    private ObjectId id;

    // 통합된 필드들 (1:1 채팅 최적화)
    @Field("room_id")
    private String roomId;  // 채팅방 ID (String)

    @Field("sender_id")
    private String senderId;  // 발신자 ID (String)

    @Field("sender_name")
    private String senderName;  // 발신자 이름

    @Field("content")
    private String content;  // 메시지 내용

    @Field("message_type")
    private String messageType;  // 메시지 타입 (TEXT, IMAGE, FILE 등)

    @Field("sent_at")
    private LocalDateTime sentAt;  // 전송 시간

    @Field("read_by")
    private List<String> readBy;  // 읽은 사용자 ID 목록

    // 기본 생성자 (Lombok이 제공)
    
    // 편의 생성자 (1:1 채팅용)
    public ChatMessage(String roomId, String senderId, String senderName, String content, String messageType) {
        this.roomId = roomId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
        this.messageType = messageType;
        this.sentAt = LocalDateTime.now();
        this.readBy = new java.util.ArrayList<>();
    }
    
    // 읽음 처리 메서드
    public void markAsRead(String userId) {
        if (readBy == null) {
            readBy = new java.util.ArrayList<>();
        }
        if (!readBy.contains(userId)) {
            readBy.add(userId);
        }
    }
    
    // 읽음 여부 확인 메서드
    public boolean isReadBy(String userId) {
        return readBy != null && readBy.contains(userId);
    }
}

