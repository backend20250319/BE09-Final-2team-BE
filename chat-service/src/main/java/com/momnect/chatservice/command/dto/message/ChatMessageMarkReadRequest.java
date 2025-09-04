package com.momnect.chatservice.command.dto.message;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ChatMessageMarkReadRequest {
    private Long userId;
    private LocalDateTime upTo; // null이면 now()
}
