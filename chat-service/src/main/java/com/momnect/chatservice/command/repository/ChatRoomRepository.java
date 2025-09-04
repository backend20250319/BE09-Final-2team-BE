package com.momnect.chatservice.command.repository;

import com.momnect.chatservice.command.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("chatRoomRepository")
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 구매자/판매자 기준 방 조회
    List<ChatRoom> findByBuyerId(Long buyerId);
    List<ChatRoom> findBySellerId(Long sellerId);

    // 상품 기준 방 단건(상품별 1:1 가정 시)
    ChatRoom findFirstByProductId(Long productId);

    // 두 유저 간의 방(1:1 기준)
    ChatRoom findFirstByBuyerIdAndSellerIdAndProductId(Long buyerId, Long sellerId, Long productId);

    boolean existsByBuyerIdAndSellerIdAndProductId(Long buyerId, Long sellerId, Long productId);
}

