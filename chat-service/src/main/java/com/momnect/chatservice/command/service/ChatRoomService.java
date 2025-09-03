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
import org.springframework.dao.DataIntegrityViolationException;
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
        
        // ✅ 기존 채팅방 확인 (동시성 문제 방지를 위해 먼저 확인)
        ChatRoom existed = chatRoomRepository
                .findFirstByBuyerIdAndSellerIdAndProductId(buyerId, sellerId, req.getProductId());
        if (existed != null) {
            log.info("기존 채팅방 발견: roomId={}, buyerId={}, sellerId={}, productId={}", 
                    existed.getId(), buyerId, sellerId, req.getProductId());
            return toResponse(existed, false);
        }

        // ✅ 채팅방 생성 시도
        ChatRoom room = ChatRoom.builder()
                .productId(req.getProductId())
                .buyerId(buyerId)
                .sellerId(sellerId)
                .createdAt(LocalDateTime.now())
                .build();

        ChatRoom saved;
        try {
            saved = chatRoomRepository.save(room);
            log.info("새 채팅방 생성 성공: roomId={}, buyerId={}, sellerId={}, productId={}", 
                    saved.getId(), buyerId, sellerId, req.getProductId());
        } catch (DataIntegrityViolationException e) {
            // ✅ 동시성 상황에서 중복 생성 시도 시 기존 방 반환
            log.warn("채팅방 중복 생성 시도 감지: buyerId={}, sellerId={}, productId={}", 
                    buyerId, sellerId, req.getProductId());
            
            // 다시 한번 기존 방 확인
            ChatRoom existingRoom = chatRoomRepository
                    .findFirstByBuyerIdAndSellerIdAndProductId(buyerId, sellerId, req.getProductId());
            if (existingRoom != null) {
                log.info("중복 생성 후 기존 채팅방 반환: roomId={}", existingRoom.getId());
                return toResponse(existingRoom, false);
            } else {
                // 예상치 못한 상황
                log.error("DataIntegrityViolationException 발생했지만 기존 방을 찾을 수 없음", e);
                throw e;
            }
        }

        // ✅ 참여자 2명 생성
        try {
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
        } catch (DataIntegrityViolationException e) {
            // ✅ 참여자 중복 생성 시도 시 무시 (이미 존재하는 경우)
            log.warn("참여자 중복 생성 시도 감지: roomId={}, buyerId={}, sellerId={}", 
                    saved.getId(), buyerId, sellerId);
        }

        return toResponse(saved, true);
        } catch (Exception e) {
            log.error("채팅방 생성 중 오류 발생: {}", e.getMessage(), e);
            throw e;
        }
    }

    /** 메시지 전송 시 채팅방이 없으면 자동 생성 (상품 ID 기반) */
    @Transactional
    public Long findOrCreateRoomForProduct(Long productId, Long userId) {
        try {
            // 상품 정보에서 sellerId 조회
            ApiResponse<List<ProductSummaryResponse>> response = productClient.getProductSummaries(List.of(productId), userId);
            if (!response.isSuccess() || response.getData() == null || response.getData().isEmpty()) {
                throw new IllegalArgumentException("상품을 찾을 수 없습니다. ID: " + productId);
            }
            List<ProductSummaryResponse> productInfos = response.getData();
        
            Long sellerId = productInfos.get(0).getSellerId();
            Long buyerId = userId; // 현재 로그인한 사용자가 buyer
            
            // 자신의 상품에 대해 채팅방을 생성하려는 경우 방지
            if (buyerId.equals(sellerId)) {
                throw new IllegalArgumentException("자신의 상품에 대해 채팅방을 생성할 수 없습니다.");
            }
            
            // 기존 채팅방 확인
            ChatRoom existed = chatRoomRepository
                    .findFirstByBuyerIdAndSellerIdAndProductId(buyerId, sellerId, productId);
            if (existed != null) {
                log.info("기존 채팅방 발견: roomId={}, buyerId={}, sellerId={}, productId={}", 
                        existed.getId(), buyerId, sellerId, productId);
                return existed.getId();
            }

            // 새 채팅방 생성
            ChatRoom room = ChatRoom.builder()
                    .productId(productId)
                    .buyerId(buyerId)
                    .sellerId(sellerId)
                    .createdAt(LocalDateTime.now())
                    .build();

            ChatRoom saved = chatRoomRepository.save(room);
            log.info("새 채팅방 자동 생성 성공: roomId={}, buyerId={}, sellerId={}, productId={}", 
                    saved.getId(), buyerId, sellerId, productId);

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

            return saved.getId();
        } catch (Exception e) {
            log.error("채팅방 자동 생성 중 오류 발생: {}", e.getMessage(), e);
            throw e;
        }
    }

    /** 내가 참여한 방 목록(최근 메시지 기준 정렬) */
    @Transactional(readOnly = true)
    public List<ChatRoomSummaryResponse> listRoomsForUser(Long userId) {
        List<ChatParticipant> parts = participantRepository.findByUserId(userId);

        return parts.stream()
                .map(p -> {
                    Long roomId = p.getChatRoomId();
                    ChatMessage last = messageRepository.findTopByRoomIdOrderBySentAtDesc(roomId.toString());

                    // ChatRoom 정보 가져오기
                    ChatRoom room = chatRoomRepository.findById(roomId).orElse(null);
                    if (room == null) {
                        return ChatRoomSummaryResponse.builder()
                                .roomId(roomId)
                                .lastMessage(last != null ? last.getContent() : null)
                                .lastSentAt(last != null ? last.getSentAt() : null)
                                .unreadCount(p.getUnreadCount())
                                .build();
                    }

                                         try {
                         // Product Service에서 상품 정보 가져오기
                         ApiResponse<List<ProductSummaryResponse>> productResponse = productClient.getProductSummaries(List.of(room.getProductId()), userId);

                         // 상대방 사용자 ID 찾기
                         Long otherUserId = room.getBuyerId().equals(userId) ? room.getSellerId() : room.getBuyerId();

                         // User Service에서 상대방 정보 가져오기
                         ApiResponse<UserBasicInfoResponse> userResponse = userServiceClient.getUserBasicInfo(otherUserId);

                         if (productResponse.isSuccess() && productResponse.getData() != null && !productResponse.getData().isEmpty()) {
                             ProductSummaryResponse productInfo = productResponse.getData().get(0);

                             ChatRoomSummaryResponse.ChatRoomSummaryResponseBuilder builder = ChatRoomSummaryResponse.builder()
                                     .roomId(roomId)
                                     .productId(room.getProductId())
                                     .productName(productInfo.getName())
                                     .productPrice(productInfo.getPrice())
                                     .productThumbnailUrl(productInfo.getThumbnailUrl())
                                     .tradeStatus(productInfo.getTradeStatus())
                                     .buyerId(room.getBuyerId())
                                     .sellerId(room.getSellerId())
                                     .lastMessage(last != null ? last.getContent() : null)
                                     .lastSentAt(last != null ? last.getSentAt() : null)
                                     .unreadCount(p.getUnreadCount())
                                     .otherUserId(otherUserId);

                             // 상대방 사용자 정보 추가
                             if (userResponse.isSuccess() && userResponse.getData() != null) {
                                 UserBasicInfoResponse userInfo = userResponse.getData();
                                 builder.otherUserNickname(userInfo.getNickname())
                                        .otherUserProfileImageUrl(userInfo.getProfileImageUrl());
                             } else {
                                 builder.otherUserNickname("상대방")
                                        .otherUserProfileImageUrl(null);
                             }

                             return builder.build();
                         }
                     } catch (Exception e) {
                         log.error("상품/사용자 정보 조회 실패 - roomId: {}, productId: {}", roomId, room.getProductId(), e);
                     }

                                         // 상품 정보 조회 실패 시에도 상대방 사용자 정보는 가져오기
                     try {
                         Long otherUserId = room.getBuyerId().equals(userId) ? room.getSellerId() : room.getBuyerId();
                         ApiResponse<UserBasicInfoResponse> userResponse = userServiceClient.getUserBasicInfo(otherUserId);

                         ChatRoomSummaryResponse.ChatRoomSummaryResponseBuilder builder = ChatRoomSummaryResponse.builder()
                                 .roomId(roomId)
                                 .productId(room.getProductId())
                                 .productName("상품명 없음")
                                 .productPrice(0)
                                 .productThumbnailUrl(null)
                                 .tradeStatus("UNKNOWN")
                                 .buyerId(room.getBuyerId())
                                 .sellerId(room.getSellerId())
                                 .lastMessage(last != null ? last.getContent() : null)
                                 .lastSentAt(last != null ? last.getSentAt() : null)
                                 .unreadCount(p.getUnreadCount())
                                 .otherUserId(otherUserId);

                         if (userResponse.isSuccess() && userResponse.getData() != null) {
                             UserBasicInfoResponse userInfo = userResponse.getData();
                             builder.otherUserNickname(userInfo.getNickname())
                                    .otherUserProfileImageUrl(userInfo.getProfileImageUrl());
                         } else {
                             builder.otherUserNickname("상대방")
                                    .otherUserProfileImageUrl(null);
                         }

                         return builder.build();
                     } catch (Exception e) {
                         log.error("사용자 정보 조회 실패 - roomId: {}, otherUserId: {}", roomId, room.getBuyerId().equals(userId) ? room.getSellerId() : room.getBuyerId(), e);

                         // 모든 정보 조회 실패 시 기본값으로 반환
                         return ChatRoomSummaryResponse.builder()
                                 .roomId(roomId)
                                 .productId(room.getProductId())
                                 .productName("상품명 없음")
                                 .productPrice(0)
                                 .productThumbnailUrl(null)
                                 .tradeStatus("UNKNOWN")
                                 .buyerId(room.getBuyerId())
                                 .sellerId(room.getSellerId())
                                 .lastMessage(last != null ? last.getContent() : null)
                                 .lastSentAt(last != null ? last.getSentAt() : null)
                                 .unreadCount(p.getUnreadCount())
                                 .otherUserId(room.getBuyerId().equals(userId) ? room.getSellerId() : room.getBuyerId())
                                 .otherUserNickname("상대방")
                                 .otherUserProfileImageUrl(null)
                                 .build();
                     }
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
