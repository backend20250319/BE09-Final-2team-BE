package com.momnect.userservice.command.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // OAuth 정보 (LOCAL, KAKAO만)
    @Column(name = "oauth_provider", nullable = false, length = 20)
    private String oauthProvider;

    @Column(name = "oauth_id", length = 100)
    private String oauthId;

    // 로그인 정보
    @Column(name = "login_id", nullable = false, unique = true, length = 30)
    private String loginId;

    @Column(name = "password", length = 255)
    private String password;

    // 기본 정보
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "nickname", length = 15)  // 15자로 설정
    private String nickname;

    @Column(name = "email", nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "phone_number", nullable = false, length = 15)
    private String phoneNumber;

    @Column(name = "address", length = 200)
    private String address;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "trade_area_ids", length = 255)
    private String tradeAreaIds; // "549,550,551" 형태로 저장

    // 권한 및 토큰
    @Column(name = "role", nullable = false, length = 10)
    @Builder.Default
    private String role = "USER";

    @Column(name = "refresh_token", length = 500)
    private String refreshToken;

    // 약관 동의
    @Column(name = "is_terms_agreed", nullable = false)
    @Builder.Default
    private Boolean isTermsAgreed = false;

    @Column(name = "is_privacy_agreed", nullable = false)
    @Builder.Default
    private Boolean isPrivacyAgreed = false;

    // 탈퇴 관련
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "is_withdrawal_agreed")
    private Boolean isWithdrawalAgreed;

    // 시스템 필드
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "updated_by", nullable = false)
    private Long updatedBy;

    // JPA 라이프사이클
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}