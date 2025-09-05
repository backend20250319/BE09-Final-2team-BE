package com.momnect.reviewservice.command.client.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class UserDTO {
    private Long id;
    private String nickname;
    private String email;
    private String profileImageUrl;
    private LocalDateTime createdAt;

    // 거래횟수 추가
    private Integer tradeCount;

    // 리뷰 개수 추가
    private Integer reviewCount;
}
