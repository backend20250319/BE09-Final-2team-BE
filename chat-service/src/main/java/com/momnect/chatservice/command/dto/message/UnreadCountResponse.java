package com.momnect.chatservice.command.dto.message;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UnreadCountResponse {
    private Long roomId;   // null이면 전체 합계 의미 (여기서는 방 단위이므로 roomId 세팅)
    private Long userId;
    private int unreadCount;
}
