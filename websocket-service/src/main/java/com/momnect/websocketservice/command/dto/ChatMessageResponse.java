package com.momnect.websocketservice.command.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {
    private String id;
    private Long chatRoomId;
    private Long senderId;
    private String message;
    private LocalDateTime sentAt;
    private boolean read;
}
