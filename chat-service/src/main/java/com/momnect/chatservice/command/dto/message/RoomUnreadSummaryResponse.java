// com/momnect/chatservice/command/dto/message/RoomUnreadSummaryResponse.java
package com.momnect.chatservice.command.dto.message;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoomUnreadSummaryResponse {
    private Long userId;
    private int totalUnread;
    private List<RoomUnread> rooms;

    // 응답 생성 시각 (UTC 기준)
    private Instant generatedAt;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RoomUnread {
        private Long roomId;
        private int unreadCount;
    }
}
