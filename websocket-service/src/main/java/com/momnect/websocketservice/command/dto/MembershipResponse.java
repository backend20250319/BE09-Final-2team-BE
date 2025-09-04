package com.momnect.websocketservice.command.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MembershipResponse {
    private boolean isMember;
    private String roomId;
    private String userId;
}
