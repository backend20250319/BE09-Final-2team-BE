// com/momnect.chatservice.command.service.ChatUnreadServiceImpl.java
package com.momnect.chatservice.command.service;

import com.momnect.chatservice.command.dto.message.RoomUnreadSummaryResponse;
import com.momnect.chatservice.command.dto.message.UnreadCountResponse;
import com.momnect.chatservice.command.entity.ChatParticipant;
import com.momnect.chatservice.command.mongo.ChatMessage;
import com.momnect.chatservice.command.repository.ChatParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatUnreadServiceImpl implements ChatUnreadService {

    /** REDIS = Source of Truth */
    private final StringRedisTemplate srt;

    /** Fallback 계산용 */
    private final ChatParticipantRepository participantRepository;
    private final MongoTemplate mongoTemplate;

    // ======================================================
    // 조회 계열: Redis 우선 → 미스 시 Mongo로 재계산 후 Redis 세팅
    // ======================================================
    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(Long roomId, Long userId) {
        String key = unreadKey(roomId, userId);

        // 1) Redis 우선
        String cached = srt.opsForValue().get(key);
        if (cached != null) {
            return UnreadCountResponse.builder()
                    .roomId(roomId)
                    .userId(userId)
                    .unreadCount(safeParseInt(cached, 0))
                    .build();
        }

        // 2) Redis 미스 → Mongo로 재계산
        int computed = computeUnreadFromMongo(roomId, userId);

        // 3) Redis 채움
        srt.opsForValue().set(key, String.valueOf(computed));

        return UnreadCountResponse.builder()
                .roomId(roomId)
                .userId(userId)
                .unreadCount(computed)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public RoomUnreadSummaryResponse getMyUnreadSummary(Long userId) {
        List<ChatParticipant> parts = participantRepository.findByUserId(userId);

        int total = 0;
        Map<Long, Integer> perRoom = new LinkedHashMap<>();

        for (ChatParticipant p : parts) {
            Long roomId = p.getChatRoomId();
            String cached = srt.opsForValue().get(unreadKey(roomId, userId));

            int count;
            if (cached != null) {
                count = safeParseInt(cached, 0);
            } else {
                count = computeUnreadFromMongo(roomId, userId);
                srt.opsForValue().set(unreadKey(roomId, userId), String.valueOf(count));
            }

            perRoom.put(roomId, count);
            total += count;
        }

        return RoomUnreadSummaryResponse.builder()
                .userId(userId)
                .totalUnread(total)
                .rooms(
                        perRoom.entrySet().stream()
                                .map(e -> RoomUnreadSummaryResponse.RoomUnread.builder()
                                        .roomId(e.getKey())
                                        .unreadCount(e.getValue())
                                        .build())
                                .collect(Collectors.toList())
                )
                .generatedAt(Instant.now())
                .build();
    }

    // ======================================================
    // 증가 계열: Redis만 수정 (DB unreadCount는 건드리지 않음)
    // ======================================================
    @Override
    @Transactional
    public void bumpUnreadForOpponents(Long roomId, Long senderId) {
        // 발신자 제외
        participantRepository.findByChatRoomId(roomId).forEach(p -> {
            if (!p.getUserId().equals(senderId)) {
                srt.opsForValue().increment(unreadKey(roomId, p.getUserId()));
            }
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getOpponentsInRoom(Long roomId, Long senderId) {
        return participantRepository.findByChatRoomId(roomId).stream()
                .map(ChatParticipant::getUserId)
                .filter(id -> !id.equals(senderId))
                .distinct()
                .collect(Collectors.toList());
    }

    // ======================================================
    // 읽음 처리: Redis 0 세팅 + DB는 lastReadAt(그리고 혼란 방지 위해 unreadCount=0)
    // ======================================================
    @Transactional
    public void markAsRead(Long roomId, Long userId, Long uptoSeq, LocalDateTime uptoTimeKstOrNull) {
        // 1) Redis: unread 0
        srt.opsForValue().set(unreadKey(roomId, userId), "0");

        // 2) DB: lastReadAt 갱신 (KST → UTC Instant) + (선택) unreadCount 0
        ChatParticipant me = participantRepository.findFirstByChatRoomIdAndUserId(roomId, userId);
        if (me != null) {
            LocalDateTime kst = (uptoTimeKstOrNull != null) ? uptoTimeKstOrNull : LocalDateTime.now();
            me.updateLastReadAt(kst);
            // DB 필드를 UI에서 참고하고 있다면 혼란 방지로 0으로 맞춤 (원치 않으면 제거)
            me.resetUnreadCount();
            participantRepository.save(me);
        }

        // Mongo 메시지 is_read=true로 마킹하는 로직은
        // ChatMessageService.markAsRead(...) 에서 이미 처리하고 있으니 중복 방지
    }

    // ======================================================
    // 내부 유틸
    // ======================================================
    private String unreadKey(Long roomId, Long userId) {
        return "room:" + roomId + ":unread:" + userId;
    }

    private int computeUnreadFromMongo(Long roomId, Long userId) {
        // 기준 시각: 참가자의 lastReadAt (없으면 null)
        ChatParticipant p = participantRepository.findFirstByChatRoomIdAndUserId(roomId, userId);

        Criteria c = new Criteria().andOperator(
                Criteria.where("chat_room_id").is(roomId),
                Criteria.where("sender_info.user_id").ne(userId) // 상대가 보낸 것만
        );

        if (p != null && p.getLastReadAt() != null) {
            Instant lastReadUtc = p.getLastReadAt().atZone(ZoneId.of("Asia/Seoul")).toInstant();
            c = new Criteria().andOperator(
                    Criteria.where("chat_room_id").is(roomId),
                    Criteria.where("sender_info.user_id").ne(userId),
                    Criteria.where("sent_at").gt(java.util.Date.from(lastReadUtc))
            );
        }

        Query q = new Query(c);
        // is_read=false 조건을 추가하고 싶으면 아래 주석 해제
        // q.addCriteria(Criteria.where("is_read").is(false));

        long cnt = mongoTemplate.count(q, ChatMessage.class);
        return (int) Math.max(0, cnt);
    }

    private int safeParseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }
}
