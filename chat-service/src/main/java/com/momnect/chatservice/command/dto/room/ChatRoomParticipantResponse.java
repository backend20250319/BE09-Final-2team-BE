package com.momnect.chatservice.command.dto.room;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ChatRoomParticipantResponse {
    private Long id;
    private Long userId;
    private int unreadCount;
    private LocalDateTime lastReadAt;
}
