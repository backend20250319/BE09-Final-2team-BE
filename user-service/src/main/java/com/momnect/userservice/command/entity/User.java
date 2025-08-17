package com.momnect.userservice.command.entity;


import jakarta.persistence.*;
import lombok.*;

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
    private Long id;

    private String oauthProvider;
    private String oauthId;

    private String address;
    private String profileImageUrl;

    private String role;

    private String name;
    private String nickname;

    private String loginId;
    private String password;

    private String email;
    private String phoneNumber;
    private String refreshToken;

    private Boolean isTermsAgreed;
    private Boolean isPrivacyAgreed;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    private Boolean isDeleted;
    private String deletionReason;
    private Boolean isWithdrawalAgreed;

    private Long updatedBy;
    private Long createdBy;
}