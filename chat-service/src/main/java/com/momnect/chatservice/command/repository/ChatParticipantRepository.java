package com.momnect.chatservice.command.repository;

import com.momnect.chatservice.command.entity.ChatParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {

    List<ChatParticipant> findByChatRoomId(Long chatRoomId);

    // 사용자의 모든 참여 방
    List<ChatParticipant> findByUserId(Long userId);

    // 특정 방에서 특정 사용자
    ChatParticipant findFirstByChatRoomIdAndUserId(Long chatRoomId, Long userId);

    // 안읽은 카운트 합계 등 처리용
    long countByChatRoomIdAndUnreadCountGreaterThan(Long chatRoomId, int zero);
}
