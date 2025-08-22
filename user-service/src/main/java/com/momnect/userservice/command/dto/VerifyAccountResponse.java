package com.momnect.userservice.command.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder
public class VerifyAccountResponse {

    private String type;
    private boolean success;
    private String message;

    // 아이디 찾기 결과
    private String loginId;

    // 비밀번호 재설정 토큰
    private String resetToken;

    public static VerifyAccountResponse findIdSuccess(String maskedLoginId) {
        return VerifyAccountResponse.builder()
                .type("FIND_ID")
                .success(true)
                .message("계정을 찾았습니다")
                .loginId(maskedLoginId)
                .build();
    }

    public static VerifyAccountResponse resetPasswordSuccess(String resetToken) {
        return VerifyAccountResponse.builder()
                .type("RESET_PASSWORD")
                .success(true)
                .message("계정이 확인되었습니다")
                .resetToken(resetToken)
                .build();
    }
}
