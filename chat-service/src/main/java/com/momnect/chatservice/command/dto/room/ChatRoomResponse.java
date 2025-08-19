package com.momnect.chatservice.command.dto.room;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ChatRoomResponse {
    private Long id;
    private Long productId;
    private Long buyerId;
    private Long sellerId;
    private LocalDateTime createdAt;
}
