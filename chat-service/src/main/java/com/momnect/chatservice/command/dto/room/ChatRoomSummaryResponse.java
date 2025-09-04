package com.momnect.chatservice.command.dto.room;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ChatRoomSummaryResponse {
    private Long roomId;
    private Long productId;
    private String productName;
    private Integer productPrice;
    private String productThumbnailUrl;
    private String tradeStatus;
    private Long buyerId;
    private Long sellerId;
    private String lastMessage;
    private LocalDateTime lastSentAt;
    private long unreadCount;
    
    // 상대방 사용자 정보
    private Long otherUserId;
    private String otherUserNickname;
    private String otherUserProfileImageUrl;
}
