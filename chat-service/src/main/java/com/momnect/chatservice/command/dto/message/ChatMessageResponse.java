package com.momnect.chatservice.command.dto.message;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ChatMessageResponse {
    private String id;          // Mongo ObjectId hex string
    private Long chatRoomId;
    private Long senderId;
    private String message;
    private LocalDateTime sentAt;
    private boolean read;
}
