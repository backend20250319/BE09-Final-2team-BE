package com.momnect.chatservice.command.dto.message;

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
