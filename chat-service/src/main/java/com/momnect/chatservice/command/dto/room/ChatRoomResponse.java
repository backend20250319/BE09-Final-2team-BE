package com.momnect.chatservice.command.dto.room;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ChatRoomResponse {
    private Long roomId;
    private Long productId;
    private String productName;
    private Integer productPrice;
    private String tradeStatus;
    private String productThumbnailUrl;
    private Long buyerId;
    private Long sellerId;
    private LocalDateTime createdAt;
}
