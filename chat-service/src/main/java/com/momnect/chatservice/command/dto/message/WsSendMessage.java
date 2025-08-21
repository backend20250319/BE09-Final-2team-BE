package com.momnect.chatservice.command.dto.message;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WsSendMessage {
    private Long roomId;
    private Long senderId;
    private String senderName;
    private String message;
}
