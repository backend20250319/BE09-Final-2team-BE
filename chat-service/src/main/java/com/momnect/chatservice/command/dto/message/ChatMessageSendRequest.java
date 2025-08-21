package com.momnect.chatservice.command.dto.message;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ChatMessageSendRequest {
    private Long senderId;
    private String senderName;
    private String message;
}

