package com.momnect.websocketservice.command.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequest {
    private String roomId;
    private String senderId;
    private String senderName;
    private String content;
    private String messageType;
}
