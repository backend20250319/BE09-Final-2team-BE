package com.momnect.chatservice.command.service;

import com.momnect.chatservice.command.dto.message.ChatMessageMarkReadRequest;
import com.momnect.chatservice.command.dto.message.ChatMessageResponse;
import com.momnect.chatservice.command.dto.message.ChatMessageSendRequest;
import com.momnect.chatservice.command.entity.ChatParticipant;
import com.momnect.chatservice.command.mongo.ChatMessage;
import com.momnect.chatservice.command.repository.ChatMessageRepository;
import com.momnect.chatservice.command.repository.ChatParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository messageRepository;
    private final ChatParticipantRepository participantRepository;
    private final MongoTemplate mongoTemplate;

    /** 메시지 전송 + 상대 unread 증가 */
    @Transactional
    public ChatMessageResponse send(Long roomId, ChatMessageSendRequest req) {
        // 1) Mongo 저장
        ChatMessage saved = messageRepository.save(ChatMessage.builder()
                .id(new ObjectId())
                .chatRoomId(roomId)
                .senderId(req.getSenderId())
                .message(req.getMessage())
                .sentAt(Instant.now())
                .isRead(false)
                .build());

        // 2) 상대 참여자 unreadCount +1 (의미 있는 도메인 메서드 사용 권장)
        List<ChatParticipant> participants = participantRepository.findByChatRoomId(roomId);
        for (ChatParticipant p : participants) {
            if (!p.getUserId().equals(req.getSenderId())) {
                p.increaseUnreadCount();
                participantRepository.save(p);
            }
        }

        return toResponse(saved);
    }

    /** 메시지 조회(최신순 페이지네이션) */
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(Long roomId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return messageRepository.findByChatRoomIdOrderBySentAtDesc(roomId, pageable)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 읽음 처리:
     * - 내 participant.lastReadAt 갱신 + unreadCount = 0
     * - (옵션) 나 아닌 발신자의 upTo 이전 메시지 is_read=true
     */
    @Transactional
    public void markAsRead(Long roomId, ChatMessageMarkReadRequest req) {
        // 1) upTo를 UTC(Instant)로 통일
        Instant upToInstant = (req.getUpTo() != null)
                // KST로 보냈다면 서버 표준(Asia/Seoul)로 해석 후 UTC로 변환
                ? req.getUpTo().atZone(ZoneId.of("Asia/Seoul")).toInstant()
                : Instant.now();

        // 2) 내 참여자 정보 갱신
        ChatParticipant me = participantRepository.findFirstByChatRoomIdAndUserId(roomId, req.getUserId());
        if (me == null) {
            throw new IllegalArgumentException("Participant not found in room: " + roomId + ", user: " + req.getUserId());
        }
        me.updateLastReadAt(req.getUpTo() != null ? req.getUpTo() : LocalDateTime.now());
        me.resetUnreadCount();
        participantRepository.save(me);

        // 3) Mongo 벌크 업데이트 (해당 방, upTo 이전, '내가 보낸 메시지' 제외)
        Query q = new Query()
                .addCriteria(Criteria.where("chat_room_id").is(roomId))
                .addCriteria(Criteria.where("sender_id").ne(req.getUserId()))
                .addCriteria(Criteria.where("sent_at").lte(Date.from(upToInstant))) // ★ 핵심: Date(UTC)로 비교
                .addCriteria(Criteria.where("is_read").is(false));

        Update update = new Update().set("is_read", true);
        var result = mongoTemplate.updateMulti(q, update, ChatMessage.class);
        // 디버깅용(선택)
        // log.info("markAsRead modifiedCount={}", result.getModifiedCount());
    }


    private ChatMessageResponse toResponse(ChatMessage m) {
        LocalDateTime sentAtLocal = null;
        if (m.getSentAt() != null) {
            sentAtLocal = LocalDateTime.ofInstant(m.getSentAt(), ZoneId.of("Asia/Seoul"));
        }
        return ChatMessageResponse.builder()
                .id(m.getId() != null ? m.getId().toHexString() : null)
                .chatRoomId(m.getChatRoomId())
                .senderId(m.getSenderId())
                .message(m.getMessage())
                .sentAt(sentAtLocal) // 응답은 KST LocalDateTime로
                .read(m.isRead())
                .build();
    }

}
