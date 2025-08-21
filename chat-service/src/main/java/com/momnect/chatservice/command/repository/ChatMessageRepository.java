package com.momnect.chatservice.command.repository;

import com.momnect.chatservice.command.mongo.ChatMessage;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, ObjectId> {

    // 방의 메시지 최신순 페이지네이션
    List<ChatMessage> findByChatRoomIdOrderBySentAtDesc(Long chatRoomId, Pageable pageable);

    // 마지막(가장 최근) 메시지
    ChatMessage findTopByChatRoomIdOrderBySentAtDesc(Long chatRoomId);

    // 안 읽은 메시지 수(상대방이 보낸 것만 집계할 때 senderId != me 로 필터링은 서비스에서)
    long countByChatRoomIdAndIsReadFalse(Long chatRoomId);

    // 시간 구간별 조회(보관/삭제 정책 등)
    List<ChatMessage> findByChatRoomIdAndSentAtBetweenOrderBySentAtAsc(Long chatRoomId,
                                                                       LocalDateTime from, LocalDateTime to);
}
