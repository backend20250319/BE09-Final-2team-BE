package com.momnect.chatservice.command.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.momnect.chatservice.command.dto.message.ChatMessageMarkReadRequest;
import com.momnect.chatservice.command.dto.message.ChatMessageResponse;
import com.momnect.chatservice.command.dto.message.ChatMessageSendRequest;
import com.momnect.chatservice.command.entity.ChatParticipant;
import com.momnect.chatservice.command.mongo.ChatMessage;
import com.momnect.chatservice.command.repository.ChatMessageRepository;
import com.momnect.chatservice.command.repository.ChatParticipantRepository;
import com.momnect.chatservice.command.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository messageRepository;
    private final ChatParticipantRepository participantRepository;
    private final MongoTemplate mongoTemplate;
    private final ChatRoomService chatRoomService;
    // private final StringRedisTemplate srt; // 임시로 Redis 의존성 제거

    private final ObjectMapper objectMapper = new ObjectMapper();

    // =========================
    // 메시지 전송 시 자동 채팅방 생성 (새로운 메서드)
    // =========================
    /** 메시지 전송 시 채팅방이 없으면 자동 생성 + 메시지 전송 */
    @Transactional
    public ChatMessageResponse sendWithAutoRoomCreation(ChatMessageSendRequest req, Long userId) {
        // 1) 채팅방이 없으면 자동 생성
        Long roomId = chatRoomService.findOrCreateRoomForProduct(req.getProductId(), userId);
        
        // 2) 기존 send 메서드 호출
        return send(roomId, req, userId);
    }

    // =========================
    // 메시지 전송 (REST에서 사용)
    // =========================
    /** 메시지 전송 + 상대 unread 증가(Participant) + Redis 카운터/이벤트 */
    @Transactional
    public ChatMessageResponse send(Long roomId, ChatMessageSendRequest req, Long userId) {
        // 1) Mongo 저장 (통합된 필드 구조 사용)
        ChatMessage saved = messageRepository.save(ChatMessage.builder()
                .id(new ObjectId())
                .roomId(roomId.toString())
                .senderId(req.getSenderId().toString())
                .senderName(req.getSenderName())
                .content(req.getMessage())
                .messageType("TEXT")
                .sentAt(LocalDateTime.now())
                .readBy(new ArrayList<>())
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
        // String seqKey = "room:" + roomId + ":seq";
        // Long seq = srt.opsForValue().increment(seqKey); // null 방지
        // if (seq == null) seq = 0L;

        // for (ChatParticipant p : participants) {
        //     if (!p.getUserId().equals(req.getSenderId())) {
        //         String unreadKey = "room:" + roomId + ":unread:" + p.getUserId();
        //         srt.opsForValue().increment(unreadKey);
        //     }
        // }

        // 4) Redis Pub/Sub: 방 채널로 이벤트 브로드캐스트 (현재 DTO 구조에 맞춤)
        // WsSendMessage evt = WsSendMessage.builder()
        //         .roomId(roomId)
        //         .senderId(req.getSenderId())
        //         .senderName(req.getSenderName())
        //         .message(req.getMessage())
        //         .build();
        // srt.convertAndSend("channel:room:" + roomId, toJson(evt));

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
                .roomId(roomId.toString())
                .senderId(senderId.toString())
                .senderName(senderName)
                .content(text)
                .messageType("TEXT")
                .sentAt(LocalDateTime.now())
                .readBy(new ArrayList<>())
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
        // String seqKey = "room:" + roomId + ":seq";
        // Long seq = srt.opsForValue().increment(seqKey);
        // if (seq == null) seq = 0L;

        // for (ChatParticipant p : participants) {
        //     if (!p.getUserId().equals(senderId)) {
        //         String unreadKey = "room:" + roomId + ":unread:" + p.getUserId();
        //         srt.opsForValue().increment(unreadKey);
        //     }
        // }

        // 4) Redis Pub/Sub (현재 WsSendMessage 구조)
        // WsSendMessage evt = WsSendMessage.builder()
        //         .roomId(roomId)
        //         .senderId(senderId)
        //         .senderName(senderName)
        //         .message(text)
        //         .build();
        // srt.convertAndSend("channel:room:" + roomId, toJson(evt));

        return toResponse(saved);
    }

    // =========================
    // 메시지 조회
    // =========================
    /** 메시지 조회(최신순 페이지네이션) */
    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(Long roomId, int page, int size, Long userId) {
        // 방 참여자인지 확인
        if (!isParticipant(roomId, userId)) {
            throw new IllegalArgumentException("You are not a participant of this room");
        }
        
        Pageable pageable = PageRequest.of(page, size);
        return messageRepository.findByRoomIdOrderBySentAtDesc(roomId.toString(), pageable)
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
    public void markAsRead(Long roomId, ChatMessageMarkReadRequest req, Long userId) throws DataAccessException {
        // 방 참여자인지 확인
        if (!isParticipant(roomId, userId)) {
            throw new IllegalArgumentException("You are not a participant of this room");
        }
        
        // 1) 내 참여자 정보 갱신 (DB)
        ChatParticipant me = participantRepository.findFirstByChatRoomIdAndUserId(roomId, req.getUserId());
        if (me == null) {
            throw new IllegalArgumentException("Participant not found in room: " + roomId + ", user: " + req.getUserId());
        }
        me.updateLastReadAt(req.getUpTo() != null ? req.getUpTo() : LocalDateTime.now());
        me.resetUnreadCount();
        participantRepository.save(me);

        // 3) Mongo 벌크 업데이트 (내가 보낸 메시지 제외 + upTo 이전)
        Query q = new Query()
                .addCriteria(Criteria.where("room_id").is(roomId.toString()))
                .addCriteria(Criteria.where("sender_id").ne(req.getUserId().toString()))
                .addCriteria(Criteria.where("sent_at").lte(req.getUpTo() != null ? req.getUpTo() : LocalDateTime.now()));
        
        // 읽음 처리: readBy 배열에 사용자 ID 추가
        List<ChatMessage> unreadMessages = mongoTemplate.find(q, ChatMessage.class);
        for (ChatMessage message : unreadMessages) {
            message.markAsRead(req.getUserId().toString());
            messageRepository.save(message);
        }

        // 4) Redis 캐시 안읽음 카운트 0으로 재설정
        // String unreadKey = "room:" + roomId + ":unread:" + req.getUserId();
        // srt.opsForValue().set(unreadKey, "0");

        // (선택) 읽음 이벤트 Pub/Sub (현재 WsSendMessage 구조엔 READ 타입이 없으니 생략/추가 가능)
        // Map<String, Object> readEvt = Map.of("type","READ_RECEIPT","roomId",roomId,"userId",req.getUserId(),"readAt",Instant.now().toString());
        // srt.convertAndSend("channel:room:" + roomId, toJson(readEvt));
    }

    // =========================
    // 변환/유틸
    // =========================
    private ChatMessageResponse toResponse(ChatMessage m) {
        // senderId는 String에서 Long으로 변환
        Long senderId = (m.getSenderId() != null) ? Long.parseLong(m.getSenderId()) : null;
        Long chatRoomId = (m.getRoomId() != null) ? Long.parseLong(m.getRoomId()) : null;

        return ChatMessageResponse.builder()
                .id(m.getId() != null ? m.getId().toHexString() : null)
                .roomId(chatRoomId)        // roomId 필드 설정
                .chatRoomId(chatRoomId)    // 기존 필드 (하위 호환성)
                .senderId(senderId)
                .message(m.getContent())
                .sentAt(m.getSentAt())
                .read(m.getReadBy() != null && !m.getReadBy().isEmpty())
                .build();
    }

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return "{}";
        }
    }
    
    /** 참여자 확인 */
    private boolean isParticipant(Long roomId, Long userId) {
        return participantRepository.findFirstByChatRoomIdAndUserId(roomId, userId) != null;
    }
}
