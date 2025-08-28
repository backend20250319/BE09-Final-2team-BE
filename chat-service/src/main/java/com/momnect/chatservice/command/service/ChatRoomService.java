package com.momnect.chatservice.command.service;

import com.momnect.chatservice.command.client.UserServiceClient;
import com.momnect.chatservice.command.client.dto.UserBasicInfoResponse;
import com.momnect.chatservice.command.client.dto.ApiResponse;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatParticipantRepository participantRepository;
    private final ChatMessageRepository messageRepository;
    private final UserServiceClient userServiceClient;

    /** 방 생성(상품별 1:1 방 중복 방지) */
    @Transactional
    public ChatRoomResponse createRoom(ChatRoomCreateRequest req, Long userId) {
        ChatRoom existed = chatRoomRepository
                .findFirstByBuyerIdAndSellerIdAndProductId(req.getBuyerId(), req.getSellerId(), req.getProductId());
        if (existed != null) return toResponse(existed, false);

        ChatRoom room = ChatRoom.builder()
                .productId(req.getProductId())
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

        return toResponse(saved, true);
    }

    /** 내가 참여한 방 목록(최근 메시지 기준 정렬) */
    @Transactional(readOnly = true)
    public List<ChatRoomSummaryResponse> listRoomsForUser(Long userId) {
        List<ChatParticipant> parts = participantRepository.findByUserId(userId);

        return parts.stream()
                .map(p -> {
                    Long roomId = p.getChatRoomId();
                    ChatMessage last = messageRepository.findTopByRoomIdOrderBySentAtDesc(roomId.toString());

                    return ChatRoomSummaryResponse.builder()
                            .roomId(roomId)
                            .lastMessage(last != null ? last.getContent() : null)
                            .lastSentAt(last != null ? last.getSentAt() : null)
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
    public List<ChatRoomParticipantResponse> getParticipants(Long roomId, Long userId) {
        // 방 참여자인지 확인
        if (!isParticipant(roomId, userId)) {
            throw new IllegalArgumentException("You are not a participant of this room");
        }
        
        return participantRepository.findByChatRoomId(roomId).stream()
                .map(p -> {
                    try {
                        // User Service에서 사용자 정보 가져오기
                        ApiResponse<UserBasicInfoResponse> response = userServiceClient.getUserBasicInfo(p.getUserId());
                        
                        if (response != null && response.isSuccess() && response.getData() != null) {
                            UserBasicInfoResponse userInfo = response.getData();
                            
                            return ChatRoomParticipantResponse.builder()
                                    .id(p.getId())
                                    .userId(p.getUserId())
                                    .nickname(userInfo.getNickname())
                                    .unreadCount(p.getUnreadCount())
                                    .lastReadAt(p.getLastReadAt())
                                    .build();
                        } else {
                            throw new RuntimeException("Failed to get user info - API response is null or unsuccessful");
                        }
                    } catch (Exception e) {
                        // 에러 발생 시 기본 정보만 반환
                        return ChatRoomParticipantResponse.builder()
                                .id(p.getId())
                                .userId(p.getUserId())
                                .nickname("사용자")
                                .unreadCount(p.getUnreadCount())
                                .lastReadAt(p.getLastReadAt())
                                .build();
                    }
                })
                .toList();
    }

    /** 단건 조회(필요 시) */
    @Transactional(readOnly = true)
    public ChatRoomResponse getRoom(Long roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("ChatRoom not found: " + roomId));
        return toResponse(room, false);
    }

    /** 참여자 확인 */
    @Transactional(readOnly = true)
    public boolean isParticipant(Long roomId, Long userId) {
        return participantRepository.findFirstByChatRoomIdAndUserId(roomId, userId) != null;
    }

    private ChatRoomResponse toResponse(ChatRoom r, boolean isNew) {
        return ChatRoomResponse.builder()
                .id(r.getId())
                .productId(r.getProductId())
                .buyerId(r.getBuyerId())
                .sellerId(r.getSellerId())
                .createdAt(r.getCreatedAt())
                .isNew(isNew)
                .build();
    }
}
