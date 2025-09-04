package com.momnect.chatservice.command.repository;

import com.momnect.chatservice.command.mongo.ChatMessage;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository("chatMessageRepository")
public interface ChatMessageRepository extends MongoRepository<ChatMessage, ObjectId> {

    // === 1:1 채팅 최적화 메서드들 ===
    
    // 방의 메시지 최신순 페이지네이션
    List<ChatMessage> findByRoomIdOrderBySentAtDesc(String roomId, Pageable pageable);

    // 마지막(가장 최근) 메시지
    ChatMessage findTopByRoomIdOrderBySentAtDesc(String roomId);

    // 특정 사용자가 읽지 않은 메시지 수
    long countByRoomIdAndReadByNotContaining(String roomId, String userId);

    // 시간 구간별 조회(보관/삭제 정책 등)
    List<ChatMessage> findByRoomIdAndSentAtBetweenOrderBySentAtAsc(String roomId,
                                                                  LocalDateTime from, LocalDateTime to);

    // 특정 발신자의 메시지 조회
    List<ChatMessage> findByRoomIdAndSenderIdOrderBySentAtDesc(String roomId, String senderId);
    
    // 특정 발신자가 보낸 메시지 중 읽지 않은 것들
    List<ChatMessage> findByRoomIdAndSenderIdAndReadByNotContainingOrderBySentAtDesc(String roomId, String senderId, String userId);
}
