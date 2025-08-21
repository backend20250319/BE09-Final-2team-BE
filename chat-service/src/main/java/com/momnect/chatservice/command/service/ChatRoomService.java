package com.momnect.chatservice.command.service;

import com.momnect.chatservice.command.dto.room.ChatRoomCreateRequest;
import com.momnect.chatservice.command.dto.room.ChatRoomParticipantResponse;
import com.momnect.chatservice.command.dto.room.ChatRoomResponse;
import com.momnect.chatservice.command.dto.room.ChatRoomSummaryResponse;
import com.momnect.chatservice.command.entity.ChatParticipant;
import com.momnect.chatservice.command.entity.ChatRoom;
import com.momnect.chatservice.command.mongo.ChatMessage;
import com.momnect.chatservice.command.repository.ChatMessageRepository;
import com.momnect.chatservice.command.repository.ChatParticipantRepository;
import com.momnect.chatservice.command.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatParticipantRepository participantRepository;
    private final ChatMessageRepository messageRepository;

    /** 방 생성(상품별 1:1 방 중복 방지) */
    @Transactional
    public ChatRoomResponse createRoom(ChatRoomCreateRequest req) {
        ChatRoom existed = chatRoomRepository
                .findFirstByBuyerIdAndSellerIdAndProductId(req.getBuyerId(), req.getSellerId(), req.getProductId());
        if (existed != null) return toResponse(existed);

        ChatRoom room = ChatRoom.builder()
                .productId(req.getProductId())   // 엔티티에서 @Column(name="product_it")
                .buyerId(req.getBuyerId())
                .sellerId(req.getSellerId())
                .createdAt(LocalDateTime.now())
                .build();

        ChatRoom saved = chatRoomRepository.save(room);

        // 참여자 2명 생성
        participantRepository.save(ChatParticipant.builder()
                .chatRoomId(saved.getId())
                .userId(req.getBuyerId())
                .unreadCount(0)
                .lastReadAt(LocalDateTime.now())
                .build());

        participantRepository.save(ChatParticipant.builder()
                .chatRoomId(saved.getId())
                .userId(req.getSellerId())
                .unreadCount(0)
                .lastReadAt(LocalDateTime.now())
                .build());

        return toResponse(saved);
    }

    /** 내가 참여한 방 목록(최근 메시지 기준 정렬) */
    @Transactional(readOnly = true)
    public List<ChatRoomSummaryResponse> listRoomsForUser(Long userId) {
        List<ChatParticipant> parts = participantRepository.findByUserId(userId);

        return parts.stream()
                .map(p -> {
                    Long roomId = p.getChatRoomId();
                    ChatMessage last = messageRepository.findTopByChatRoomIdOrderBySentAtDesc(roomId);

                    // 응답 DTO가 LocalDateTime(KST)라면 변환
                    LocalDateTime lastSentAtKst = null;
                    if (last != null && last.getSentAt() != null) {
                        lastSentAtKst = LocalDateTime.ofInstant(last.getSentAt(), ZoneId.of("Asia/Seoul"));
                    }

                    return ChatRoomSummaryResponse.builder()
                            .roomId(roomId)
                            .lastMessage(last != null ? last.getMessage() : null)
                            .lastSentAt(lastSentAtKst)   // DTO 필드 타입이 LocalDateTime이라면 그대로
                            .unreadCount(p.getUnreadCount())
                            .build();
                })
                .sorted(Comparator.comparing(
                        ChatRoomSummaryResponse::getLastSentAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();
    }

    /** 방 참여자 목록 */
    @Transactional(readOnly = true)
    public List<ChatRoomParticipantResponse> getParticipants(Long roomId) {
        return participantRepository.findByChatRoomId(roomId).stream()
                .map(p -> ChatRoomParticipantResponse.builder()
                        .id(p.getId())
                        .userId(p.getUserId())
                        .unreadCount(p.getUnreadCount())
                        .lastReadAt(p.getLastReadAt())
                        .build())
                .toList();
    }

    /** 단건 조회(필요 시) */
    @Transactional(readOnly = true)
    public ChatRoomResponse getRoom(Long roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("ChatRoom not found: " + roomId));
        return toResponse(room);
    }

    private ChatRoomResponse toResponse(ChatRoom r) {
        return ChatRoomResponse.builder()
                .id(r.getId())
                .productId(r.getProductId())
                .buyerId(r.getBuyerId())
                .sellerId(r.getSellerId())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
