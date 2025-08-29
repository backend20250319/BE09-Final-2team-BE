package com.momnect.userservice.command.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ResetPasswordRequest {

    @NotBlank(message = "재설정 토큰을 입력해주세요")
    private final String resetToken;

    @NotBlank(message = "새 비밀번호를 입력해주세요")
    @Size(min = 8, max = 16, message = "비밀번호는 8-16자리로 입력해주세요")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&*(),.?\"{}|<>]).{8,16}$",
            message = "비밀번호는 영문자, 숫자, 특수문자를 포함해야 합니다")
    private final String newPassword;

    @NotBlank(message = "비밀번호 확인을 입력해주세요")
    private final String newPasswordConfirm;

    public boolean isPasswordMatched() {
        return newPassword != null && newPassword.equals(newPasswordConfirm);
    }
}
