package com.momnect.userservice.command.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UpdateProfileRequest {

    @Size(max = 15, message = "닉네임은 15자 이하로 입력해주세요")
    private final String nickname;

    @Email(message = "올바른 이메일 형식으로 입력해주세요")
    @Size(max = 100, message = "이메일은 100자 이하로 입력해주세요")
    private final String email;

    @Pattern(regexp = "^010[0-9]{8}$", message = "휴대전화번호는 010으로 시작하는 11자리 숫자로 입력해주세요")
    private final String phoneNumber;

    private String profileImageUrl;
}
