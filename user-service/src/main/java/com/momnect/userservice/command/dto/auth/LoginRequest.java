package com.momnect.userservice.command.dto.auth;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class LoginRequest {
    private final String loginId;
    private final String password;
}
