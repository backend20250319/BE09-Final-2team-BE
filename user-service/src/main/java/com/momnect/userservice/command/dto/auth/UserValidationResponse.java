package com.momnect.userservice.command.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserValidationResponse {
    private boolean valid;
    private String userId;
    private String username;
    private List<String> roles;
}
