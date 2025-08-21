// com/momnect/chatservice/command/dto/message/RoomUnreadSummaryResponse.java
package com.momnect.chatservice.command.dto.message;

import lombok.*;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoomUnreadSummaryResponse {
    private Long userId;
    private int totalUnread;
    private List<RoomUnread> rooms;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RoomUnread {
        private Long roomId;
        private int unreadCount;
    }
}
