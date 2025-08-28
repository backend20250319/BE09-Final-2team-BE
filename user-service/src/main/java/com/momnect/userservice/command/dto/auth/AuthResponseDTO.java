package com.momnect.userservice.command.dto.auth;

import com.momnect.userservice.command.dto.child.ChildDTO;
import com.momnect.userservice.command.dto.user.PublicUserDTO;
import com.momnect.userservice.command.dto.common.TradeLocationDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {
    private String accessToken;
    private String refreshToken;
    private PublicUserDTO user;
    private List<ChildDTO> childList;
    private List<TradeLocationDTO> tradeLocationList;
}