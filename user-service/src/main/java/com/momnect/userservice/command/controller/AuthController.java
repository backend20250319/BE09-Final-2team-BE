package com.momnect.userservice.command.controller;

import com.momnect.userservice.command.dto.LoginRequest;
import com.momnect.userservice.command.dto.LoginResponse;
import com.momnect.userservice.command.dto.SignupRequest;
import com.momnect.userservice.command.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "Auth API", description = "유저 인증 관련 기능을 제공합니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    /**
     * 회원가입
     * @param request 회원가입 요청 DTO
     * @return 로그인 후 발급되는 AccessToken + RefreshToken + UserDTO
     */
    @PostMapping("/signup")
    public LoginResponse signup(@RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    /**
     * 로그인
     *
     * @param request 로그인 요청 DTO
     * @return AccessToken + RefreshToken + UserDTO
     */
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request.getLoginId(), request.getPassword());
    }
}
