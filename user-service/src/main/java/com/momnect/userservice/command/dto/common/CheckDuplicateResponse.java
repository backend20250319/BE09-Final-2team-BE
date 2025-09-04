package com.momnect.userservice.command.dto.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CheckDuplicateResponse {
    private final String type;          // "loginId", "nickname", "email"
    private final String value;         // 확인한 값
    private final boolean isDuplicate;  // 중복 여부
    private final String message;

    public CheckDuplicateResponse(String type, String value, boolean isDuplicate) {
        this.type = type;
        this.value = value;
        this.isDuplicate = isDuplicate;
        this.message = generateMessage(type, isDuplicate);
    }

    private String generateMessage(String type, boolean isDuplicate) {
        if (isDuplicate) {
            return switch (type.toLowerCase()) {
                case "loginid" -> "이미 사용 중인 로그인 ID입니다.";
                case "nickname" -> "이미 사용 중인 닉네임입니다.";
                case "email" -> "이미 사용 중인 이메일입니다.";
                default -> "중복된 값입니다.";
            };
        } else {
            return switch (type.toLowerCase()) {
                case "loginid" -> "사용 가능한 로그인 ID입니다.";
                case "nickname" -> "사용 가능한 닉네임입니다.";
                case "email" -> "사용 가능한 이메일입니다.";
                default -> "사용 가능합니다.";
            };
        }
    }
}