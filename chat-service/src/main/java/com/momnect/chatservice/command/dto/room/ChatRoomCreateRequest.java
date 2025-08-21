package com.momnect.chatservice.command.dto.room;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ChatRoomCreateRequest {
    private Long productId;
    private Long buyerId;
    private Long sellerId;
}
