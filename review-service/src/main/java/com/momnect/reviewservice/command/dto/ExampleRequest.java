package com.momnect.reviewservice.command.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ExampleRequest {
    private final String loginId;
    private final String password;
}
