package com.momnect.chatservice.command.dto.room;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ChatRoomSummaryResponse {
    private Long roomId;
    private String lastMessage;
    private LocalDateTime lastSentAt;
    private long unreadCount;
}
