package com.momnect.postservice.command.dto;

import com.momnect.postservice.command.dto.ExampleDTO;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ExampleResponse {
    private final String accessToken;
    private final String refreshToken;
    private final ExampleDTO user;
}
