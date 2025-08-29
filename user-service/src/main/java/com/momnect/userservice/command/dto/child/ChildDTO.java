package com.momnect.userservice.command.dto.child;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class ChildDTO {
    private Long id;           // 자녀 ID
    private Long userId;       // 부모 ID
    private String nickname;   // 애칭
    private LocalDate birthDate; // 생년월일
    private Integer age;       // 만 나이 (자동 계산)
}