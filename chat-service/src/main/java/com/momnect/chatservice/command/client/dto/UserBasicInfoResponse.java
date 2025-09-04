package com.momnect.chatservice.command.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBasicInfoResponse {
    private Long id;
    private String nickname;
    private String profileImageUrl;
}
