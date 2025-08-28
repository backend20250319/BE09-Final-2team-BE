package com.momnect.userservice.command.dto.child;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


import java.time.LocalDate;

@Getter
@RequiredArgsConstructor
public class CreateChildRequest {

    @NotBlank(message = "자녀 애칭을 입력해주세요")
    @Size(max = 15, message = "애칭은 15자 이하로 입력해주세요")
    private final String nickname;

    @NotNull(message = "생년월일을 입력해주세요")
    private final LocalDate birthDate;
}