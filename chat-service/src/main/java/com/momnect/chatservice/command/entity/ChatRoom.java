package com.momnect.chatservice.command.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tbl_chat_room", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"buyer_id", "seller_id", "product_id"})
})
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 상품 ID (외부 참조)
    @Column(name = "product_id", nullable = false)
    private Long productId;

    // 구매자
    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    // 판매자
    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
