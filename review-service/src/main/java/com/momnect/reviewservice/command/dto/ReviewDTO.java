package com.momnect.reviewservice.command.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// FeignClient를 통해 Product Service로부터 받아올 사용자 정보 DTO
@Getter
@Builder
public class ReviewDTO {

    private Long id;
    private String oauthProvider;
    private String oauthId;
    private String role;
    private String name;
    private String nickname;
    private String loginId;
    private String email;
    private String phoneNumber;
    private String profileImageUrl;
    private String address;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Boolean isDeleted;
    private String deletionReason;
    private Boolean isTermsAgreed;
    private Boolean isPrivacyAgreed;
    private Boolean isWithdrawalAgreed;
    private Long createdBy;
    private Long updatedBy;
}
