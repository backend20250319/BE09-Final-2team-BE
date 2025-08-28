package com.momnect.userservice.command.mapper;

import com.momnect.userservice.command.dto.user.UserDTO;
import com.momnect.userservice.command.dto.user.PublicUserDTO;
import com.momnect.userservice.command.entity.User;


import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * User 엔티티와 DTO 간 변환을 담당하는 매퍼 클래스
 * 중복 코드 제거 및 일관된 변환 로직 제공
 */
@Component
public class UserMapper {

    /**
     * User 엔티티를 UserDTO로 변환 (완전한 정보)
     * 본인 정보 조회 시 사용
     */
    public UserDTO toUserDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .loginId(user.getLoginId())
                .name(user.getName())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .profileImageUrl(user.getProfileImageUrl())
                .role(user.getRole())
                .oauthProvider(user.getOauthProvider())
                .oauthId(user.getOauthId())
                .isTermsAgreed(user.getIsTermsAgreed())
                .isPrivacyAgreed(user.getIsPrivacyAgreed())
                .isWithdrawalAgreed(user.getIsWithdrawalAgreed())
                .isDeleted(user.getIsDeleted())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .deletedAt(user.getDeletedAt())
                .createdBy(user.getCreatedBy())
                .updatedBy(user.getUpdatedBy())
                .build();
    }

    /**
     * User 엔티티를 PublicUserDTO로 변환 (공개 정보만)
     * 다른 사용자 프로필 조회 시 사용
     */
    public PublicUserDTO toPublicUserDTO(User user, List<String> tradeLocations) {
        return PublicUserDTO.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .createdAt(user.getCreatedAt())
                .tradeLocations(tradeLocations)
                .build();
    }

    // 오버로딩: tradeLocations 없이 변환할 때
    public PublicUserDTO toPublicUserDTO(User user) {
        return toPublicUserDTO(user, Collections.emptyList());
    }
}