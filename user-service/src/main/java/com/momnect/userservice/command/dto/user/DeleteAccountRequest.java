package com.momnect.userservice.command.dto.user;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class DeleteAccountRequest {

    private final String password; // 소셜 로그인 시 null 가능

    @NotNull(message = "탈퇴 동의는 필수입니다")
    private final Boolean isWithdrawalAgreed;
}
