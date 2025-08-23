package com.momnect.chatservice.command.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.momnect.chatservice.command.dto.message.ChatMessageMarkReadRequest;
import com.momnect.chatservice.command.dto.message.ChatMessageResponse;
import com.momnect.chatservice.command.dto.message.ChatMessageSendRequest;
import com.momnect.chatservice.command.dto.message.WsSendMessage;
import com.momnect.chatservice.command.entity.ChatParticipant;
import com.momnect.chatservice.command.mongo.ChatMessage;
import com.momnect.chatservice.command.mongo.SenderInfo;
import com.momnect.chatservice.command.repository.ChatMessageRepository;
import com.momnect.chatservice.command.repository.ChatParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private final StringRedisTemplate srt;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // =========================
    // 메시지 전송 (REST에서 사용)
    // =========================
    /** 메시지 전송 + 상대 unread 증가(Participant) + Redis 카운터/이벤트 */
    @Transactional
    public ChatMessageResponse send(Long roomId, ChatMessageSendRequest req) {
        // 1) Mongo 저장 (senderInfo에 보낸 사람 정보 채워넣기)
        ChatMessage saved = messageRepository.save(ChatMessage.builder()
                .id(new ObjectId())
                .chatRoomId(roomId)
                .senderInfo(SenderInfo.builder()
                        .userId(req.getSenderId())
                        .username(req.getSenderName())
                        .build())
                .message(req.getMessage())
                .sentAt(Instant.now())                     // UTC(Instant)로 저장
                .isRead(false)
                .build());

        // 2) 상대 참여자 unreadCount +1 (DB)
        List<ChatParticipant> participants = participantRepository.findByChatRoomId(roomId);
        for (ChatParticipant p : participants) {
            if (!p.getUserId().equals(req.getSenderId())) {
                p.increaseUnreadCount();
                participantRepository.save(p);
            }
        }

        // 3) Redis: seq 증가 & (옵션) 상대 unread 카운트도 증가 (캐시/실시간 뱃지용)
        String seqKey = "room:" + roomId + ":seq";
        Long seq = srt.opsForValue().increment(seqKey); // null 방지
        if (seq == null) seq = 0L;

        for (ChatParticipant p : participants) {
            if (!p.getUserId().equals(req.getSenderId())) {
                String unreadKey = "room:" + roomId + ":unread:" + p.getUserId();
                srt.opsForValue().increment(unreadKey);
            }
        }

        // 4) Redis Pub/Sub: 방 채널로 이벤트 브로드캐스트 (현재 DTO 구조에 맞춤)
        WsSendMessage evt = WsSendMessage.builder()
                .roomId(roomId)
                .senderId(req.getSenderId())
                .senderName(req.getSenderName())
                .message(req.getMessage())
                .build();
        srt.convertAndSend("channel:room:" + roomId, toJson(evt));

        // 응답
        return toResponse(saved);
    }

    // =========================
    // 메시지 전송 (WS에서 사용하기 쉬운 오버로드)
    // =========================
    /**
     * WS 핸들러에서 간편 사용: senderId/receiverId/text 형태
     * - 저장/카운팅/Publish 로직은 위 REST와 동일
     */
    @Transactional
    public ChatMessageResponse send(Long roomId, Long senderId, Long receiverId, String text) {
        // senderName을 알 수 있으면 채워주고, 없으면 null
        String senderName = null;

        // 1) Mongo 저장
        ChatMessage saved = messageRepository.save(ChatMessage.builder()
                .id(new ObjectId())
                .chatRoomId(roomId)
                .senderInfo(SenderInfo.builder()
                        .userId(senderId)
                        .username(senderName)
                        .build())
                .message(text)
                .sentAt(Instant.now())
                .isRead(false)
                .build());

        // 2) DB 참여자 unread 증가
        List<ChatParticipant> participants = participantRepository.findByChatRoomId(roomId);
        for (ChatParticipant p : participants) {
            if (!p.getUserId().equals(senderId)) {
                p.increaseUnreadCount();
                participantRepository.save(p);
            }
        }

        // 3) Redis: seq 증가 + 수신자(들) unread 증가
        String seqKey = "room:" + roomId + ":seq";
        Long seq = srt.opsForValue().increment(seqKey);
        if (seq == null) seq = 0L;

        for (ChatParticipant p : participants) {
            if (!p.getUserId().equals(senderId)) {
                String unreadKey = "room:" + roomId + ":unread:" + p.getUserId();
                srt.opsForValue().increment(unreadKey);
            }
        }

        // 4) Redis Pub/Sub (현재 WsSendMessage 구조)
        WsSendMessage evt = WsSendMessage.builder()
                .roomId(roomId)
                .senderId(senderId)
                .senderName(senderName)
                .message(text)
                .build();
        srt.convertAndSend("channel:room:" + roomId, toJson(evt));

        return toResponse(saved);
    }

    // =========================
    // 메시지 조회
    // =========================
    /** 메시지 조회(최신순 페이지네이션) */
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(Long roomId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return messageRepository.findByChatRoomIdOrderBySentAtDesc(roomId, pageable)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =========================
    // 읽음 처리
    // =========================
    /**
     * 읽음 처리:
     * - 내 participant.lastReadAt 갱신 + unreadCount = 0 (DB)
     * - Mongo: 내가 아닌 발신자의 upTo 이전 메시지 is_read=true
     * - Redis: lastReadSeq가 없다(엔티티에 seq 필드가 없으므로), 캐시 카운트만 0으로 리셋
     */
    @Transactional
    public void markAsRead(Long roomId, ChatMessageMarkReadRequest req) throws DataAccessException {
        // 1) upTo를 UTC(Instant)로 통일
        Instant upToInstant = (req.getUpTo() != null)
                ? req.getUpTo().atZone(ZoneId.of("Asia/Seoul")).toInstant()
                : Instant.now();

        // 2) 내 참여자 정보 갱신 (DB)
        ChatParticipant me = participantRepository.findFirstByChatRoomIdAndUserId(roomId, req.getUserId());
        if (me == null) {
            throw new IllegalArgumentException("Participant not found in room: " + roomId + ", user: " + req.getUserId());
        }
        me.updateLastReadAt(req.getUpTo() != null ? req.getUpTo() : LocalDateTime.now());
        me.resetUnreadCount();
        participantRepository.save(me);

        // 3) Mongo 벌크 업데이트 (내가 보낸 메시지 제외 + upTo 이전)
        Query q = new Query()
                .addCriteria(Criteria.where("chat_room_id").is(roomId))
                .addCriteria(Criteria.where("sender_info.user_id").ne(req.getUserId()))
                .addCriteria(Criteria.where("sent_at").lte(Date.from(upToInstant)))
                .addCriteria(Criteria.where("is_read").is(false));
        Update update = new Update().set("is_read", true);
        mongoTemplate.updateMulti(q, update, ChatMessage.class);

        // 4) Redis 캐시 안읽음 카운트 0으로 재설정
        String unreadKey = "room:" + roomId + ":unread:" + req.getUserId();
        srt.opsForValue().set(unreadKey, "0");

        // (선택) 읽음 이벤트 Pub/Sub (현재 WsSendMessage 구조엔 READ 타입이 없으니 생략/추가 가능)
        // Map<String, Object> readEvt = Map.of("type","READ_RECEIPT","roomId",roomId,"userId",req.getUserId(),"readAt",Instant.now().toString());
        // srt.convertAndSend("channel:room:" + roomId, toJson(readEvt));
    }

    // =========================
    // 변환/유틸
    // =========================
    private ChatMessageResponse toResponse(ChatMessage m) {
        LocalDateTime sentAtLocal = null;
        if (m.getSentAt() != null) {
            sentAtLocal = LocalDateTime.ofInstant(m.getSentAt(), ZoneId.of("Asia/Seoul"));
        }

        // senderId는 senderInfo.userId에서 추출
        Long senderId = (m.getSenderInfo() != null) ? m.getSenderInfo().getUserId() : null;

        return ChatMessageResponse.builder()
                .id(m.getId() != null ? m.getId().toHexString() : null)
                .chatRoomId(m.getChatRoomId())
                .senderId(senderId)
                .message(m.getMessage())
                .sentAt(sentAtLocal)
                .read(m.isRead())
                .build();
    }

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return "{}";
        }
    }
}
