package com.momnect.chatservice.command.dto.room;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ChatRoomCreateRequest {
    private Long productId;
    // buyerId와 sellerId는 제거 - 자동으로 설정됨
    // buyerId: 현재 로그인한 사용자 ID
    // sellerId: 상품 정보에서 자동 조회
}
