package com.momnect.chatservice.command.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tbl_chat_participant")
public class ChatParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 채팅방 FK
    @Column(name = "chat_room_id", nullable = false)
    private Long chatRoomId;

    // 유저 FK
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 읽지 않은 메시지 수
    @Column(name = "unread_count", nullable = false)
    private int unreadCount;

    @Column(name = "last_read_at", nullable = false)
    private LocalDateTime lastReadAt;

    /** ===== 도메인 메서드 ===== */

    /** 안 읽은 메시지 +1 */
    public void increaseUnreadCount() {
        this.unreadCount++;
    }

    /** 안 읽은 메시지 초기화 */
    public void resetUnreadCount() {
        this.unreadCount = 0;
    }

    /** 마지막 읽은 시간 갱신 */
    public void updateLastReadAt(LocalDateTime upTo) {
        this.lastReadAt = upTo != null ? upTo : LocalDateTime.now();
    }
}
