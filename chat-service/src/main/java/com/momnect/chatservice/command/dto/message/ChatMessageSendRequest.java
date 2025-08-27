package com.momnect.chatservice.command.dto.message;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ChatMessageSendRequest {
    private Long senderId;
    private String senderName;
    
    @JsonAlias({"message", "content"})
    private String message;
}

