package com.momnect.userservice.command.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SignupRequest {
    private final String loginId;
    private final String password;
    private final String name;
    private final String role; // USER / ADMIN
    private final String oauthProvider; // LOCAL, KAKAO, NAVER...
}