package com.momnect.userservice.command.dto.user;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PublicUserDTO {
    // 타 사용자 프로필용
    private Long id;
    private String nickname;
    private String email;
    private String profileImageUrl;
    private LocalDateTime createdAt;
    private List<String> tradeLocations;
}
