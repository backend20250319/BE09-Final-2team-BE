package com.momnect.chatservice.command.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ExampleResponse {
    private final String accessToken;
    private final String refreshToken;
    private final ExampleDTO user;
}
