package com.momnect.chatservice.command.dto.message;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ChatMessageSendRequest {
    private Long senderId;
    private String senderName;
    private Long productId; // 상품 ID 추가
    
    @JsonAlias({"message", "content"})
    private String message;
}

