package com.momnect.userservice.command.dto.child;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@Getter
@RequiredArgsConstructor
public class UpdateChildRequest {

    @Size(max = 15, message = "애칭은 15자 이하로 입력해주세요")
    private final String nickname;  // null 가능 (부분 업데이트)

    private final LocalDate birthDate;  // null 가능 (부분 업데이트)
}