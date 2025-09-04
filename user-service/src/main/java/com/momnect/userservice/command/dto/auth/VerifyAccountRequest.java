package com.momnect.userservice.command.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class VerifyAccountRequest {

    @NotNull(message = "요청 타입은 필수입니다")
    private final String type; // "FIND_ID", "RESET_PASSWORD"

    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private final String email;

    // 아이디 찾기용 (type=FIND_ID일 때 필요)
    private final String name;

    // 비밀번호 찾기용 (type=RESET_PASSWORD일 때 필요)
    private final String loginId;
}
