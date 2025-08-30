package com.momnect.chatservice.command.service;

import com.momnect.chatservice.command.client.UserServiceClient;
import com.momnect.chatservice.command.client.ProductClient;
import com.momnect.chatservice.command.client.dto.UserBasicInfoResponse;
import com.momnect.chatservice.command.client.dto.ProductSummaryResponse;
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
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatParticipantRepository participantRepository;
    private final ChatMessageRepository messageRepository;
    private final UserServiceClient userServiceClient;
    private final ProductClient productClient;

    /** 방 생성(상품별 1:1 방 중복 방지) */
    @Transactional
    public ChatRoomResponse createRoom(ChatRoomCreateRequest req, Long userId) {
        try {
            // 상품 정보에서 sellerId 조회
            ApiResponse<List<ProductSummaryResponse>> response = productClient.getProductSummaries(List.of(req.getProductId()), userId);
            if (!response.isSuccess() || response.getData() == null || response.getData().isEmpty()) {
                throw new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + req.getProductId());
            }
            List<ProductSummaryResponse> productInfos = response.getData();
        
        Long sellerId = productInfos.get(0).getSellerId();
        Long buyerId = userId; // 현재 로그인한 사용자가 buyer
        
        // 자신의 상품에 대해 채팅방을 생성하려는 경우 방지
        if (buyerId.equals(sellerId)) {
            throw new IllegalArgumentException("자신의 상품에 대해 채팅방을 생성할 수 없습니다.");
        }
        
        ChatRoom existed = chatRoomRepository
                .findFirstByBuyerIdAndSellerIdAndProductId(buyerId, sellerId, req.getProductId());
        if (existed != null) return toResponse(existed, false);

        ChatRoom room = ChatRoom.builder()
                .productId(req.getProductId())
                .buyerId(buyerId)
                .sellerId(sellerId)
                .createdAt(LocalDateTime.now())
                .build();

        ChatRoom saved = chatRoomRepository.save(room);

        // 참여자 2명 생성
        participantRepository.save(ChatParticipant.builder()
                .chatRoomId(saved.getId())
                .userId(buyerId)
                .unreadCount(0)
                .lastReadAt(LocalDateTime.now())
                .build());

        participantRepository.save(ChatParticipant.builder()
                .chatRoomId(saved.getId())
                .userId(sellerId)
                .unreadCount(0)
                .lastReadAt(LocalDateTime.now())
                .build());

        return toResponse(saved, true);
        } catch (Exception e) {
            log.error("채팅방 생성 중 오류 발생: {}", e.getMessage(), e);
            throw e;
        }
    }

    /** 내가 참여한 방 목록(최근 메시지 기준 정렬) */
    @Transactional(readOnly = true)
    public List<ChatRoomSummaryResponse> listRoomsForUser(Long userId) {
        List<ChatParticipant> parts = participantRepository.findByUserId(userId);

        // 참여한 방 → ChatRoom 조회
        List<ChatRoom> rooms = parts.stream()
                .map(p -> chatRoomRepository.findById(p.getChatRoomId()).orElse(null))
                .filter(Objects::nonNull)
                .toList();

        // 1. productId, userId 중복 제거
        Set<Long> productIds = rooms.stream()
                .map(ChatRoom::getProductId)
                .collect(Collectors.toSet());

        Set<Long> userIds = rooms.stream()
                .map(r -> r.getBuyerId().equals(userId) ? r.getSellerId() : r.getBuyerId())
                .collect(Collectors.toSet());

        // 2. 캐싱 Map
        Map<Long, ProductSummaryResponse> productMap = new HashMap<>();
        for (Long pid : productIds) {
            try {
                var res = productClient.getProductSummaries(List.of(pid), userId);
                if (res.isSuccess() && res.getData() != null && !res.getData().isEmpty()) {
                    productMap.put(pid, res.getData().get(0));
                }
            } catch (Exception e) {
                log.error("상품 조회 실패 productId={}", pid, e);
            }
        }

        Map<Long, UserBasicInfoResponse> userMap = new HashMap<>();
        for (Long uid : userIds) {
            try {
                var res = userServiceClient.getUserBasicInfo(uid);
                if (res.isSuccess() && res.getData() != null) {
                    userMap.put(uid, res.getData());
                }
            } catch (Exception e) {
                log.error("사용자 조회 실패 userId={}", uid, e);
            }
        }

        // 3. 최종 응답 생성
        return parts.stream()
                .map(p -> {
                    ChatRoom room = rooms.stream()
                            .filter(r -> r.getId().equals(p.getChatRoomId()))
                            .findFirst().orElse(null);
                    if (room == null) return null;

                    Long otherUserId = room.getBuyerId().equals(userId) ? room.getSellerId() : room.getBuyerId();
                    ChatMessage last = messageRepository.findTopByRoomIdOrderBySentAtDesc(room.getId().toString());

                    ProductSummaryResponse productInfo = productMap.get(room.getProductId());
                    UserBasicInfoResponse userInfo = userMap.get(otherUserId);

                    return ChatRoomSummaryResponse.builder()
                            .roomId(room.getId())
                            .productId(room.getProductId())
                            .productName(productInfo != null ? productInfo.getName() : "상품명 없음")
                            .productPrice(productInfo != null ? productInfo.getPrice() : 0)
                            .productThumbnailUrl(productInfo != null ? productInfo.getThumbnailUrl() : null)
                            .tradeStatus(productInfo != null ? productInfo.getTradeStatus() : "UNKNOWN")
                            .buyerId(room.getBuyerId())
                            .sellerId(room.getSellerId())
                            .lastMessage(last != null ? last.getContent() : null)
                            .lastSentAt(last != null ? last.getSentAt() : null)
                            .unreadCount(p.getUnreadCount())
                            .otherUserId(otherUserId)
                            .otherUserNickname(userInfo != null ? userInfo.getNickname() : "상대방")
                            .otherUserProfileImageUrl(userInfo != null ? userInfo.getProfileImageUrl() : null)
                            .build();
                })
                .filter(Objects::nonNull)
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
        try {
            // Product Service에서 상품 정보 가져오기
            // toResponse에서는 userId가 없으므로 null로 전달
            ApiResponse<List<ProductSummaryResponse>> response = productClient.getProductSummaries(List.of(r.getProductId()), null);
            if (!response.isSuccess() || response.getData() == null || response.getData().isEmpty()) {
                throw new RuntimeException("Product info not found");
            }
            List<ProductSummaryResponse> productInfos = response.getData();
            
            ProductSummaryResponse productInfo = productInfos.get(0);
                
            return ChatRoomResponse.builder()
                    .roomId(r.getId())
                    .productId(r.getProductId())
                    .productName(productInfo.getName())
                    .productPrice(productInfo.getPrice())
                    .tradeStatus(productInfo.getTradeStatus())
                    .productThumbnailUrl(productInfo.getThumbnailUrl())
                    .buyerId(r.getBuyerId())
                    .sellerId(r.getSellerId())
                    .createdAt(r.getCreatedAt())
                    .build();
        } catch (Exception e) {
            // 에러 발생 시 기본 정보만 반환
            return ChatRoomResponse.builder()
                    .roomId(r.getId())
                    .productId(r.getProductId())
                    .productName("상품명 없음")
                    .productPrice(0)
                    .tradeStatus("UNKNOWN")
                    .productThumbnailUrl(null)
                    .buyerId(r.getBuyerId())
                    .sellerId(r.getSellerId())
                    .createdAt(r.getCreatedAt())
                    .build();
        }
    }
}
