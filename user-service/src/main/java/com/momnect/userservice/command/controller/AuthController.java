package com.momnect.userservice.command.controller;

import com.momnect.userservice.command.dto.auth.*;
import com.momnect.userservice.command.service.AuthService;
import com.momnect.userservice.common.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

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
    public ResponseEntity<ApiResponse<AuthResponseDTO>> signup(@Valid @RequestBody SignupRequest request) { // DTO 사용
        AuthResponseDTO authResponse = authService.signup(request); // DTO 전달

        // HttpOnly 쿠키 설정
        ResponseCookie accessTokenCookie = createAccessTokenCookie(authResponse.getAccessToken());
        ResponseCookie refreshTokenCookie = createRefreshTokenCookie(authResponse.getRefreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(ApiResponse.success(authResponse)); // 전체 authResponse 반환 (토큰 포함)
    }

    /**
     * 로그인
     *
     * @param request 로그인 요청 DTO
     * @return AccessToken + RefreshToken + UserDTO
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> login(@Valid @RequestBody LoginRequest request) { // DTO
        AuthResponseDTO authResponse = authService.login(request);

        // HttpOnly 쿠키 설정
        ResponseCookie accessTokenCookie = createAccessTokenCookie(authResponse.getAccessToken());
        ResponseCookie refreshTokenCookie = createRefreshTokenCookie(authResponse.getRefreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(ApiResponse.success(authResponse)); // 전체 authResponse 반환 (토큰 포함)
    }

    /**
     * 로그아웃
     *
     * @param request 로그인 요청 DTO
     * @return AccessToken + RefreshToken + UserDTO
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        String userId = request.getHeader("X-User-Id");
        if(userId != null) {
            authService.logout(Long.parseLong(userId));
        }

        // 쿠키 삭제
        ResponseCookie accessTokenCookie = createExpiredCookie("accessToken");
        ResponseCookie refreshTokenCookie = createExpiredCookie("refreshToken");

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(ApiResponse.success(null));
    }

    /**
     * 통합 계정 확인 (아이디 찾기 / 비밀번호 재설정)
     */
    @PostMapping("/verify-account")
    public ResponseEntity<ApiResponse<VerifyAccountResponse>> verifyAccount(
            @Valid @RequestBody VerifyAccountRequest request) {

        VerifyAccountResponse response = authService.verifyAccount(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

        /**
         * 비밀번호 재설정
         */
    @PutMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("비밀번호가 성공적으로 변경되었습니다"));
    }

    /**
     * 토큰 재발급 (AccessToken 갱신)
     *
     * @param refreshToken HttpOnly 쿠키에서 추출한 RefreshToken
     * @return 새로운 AccessToken이 설정된 응답 (쿠키)
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Void>> refreshToken(@CookieValue(name = "refreshToken", required = false) String refreshToken) {

        if(refreshToken == null) {
            throw new RuntimeException("refreshToken is null");
        }

        String newAccessToken = authService.refreshAccessToken(refreshToken);

        // 새로운 AccessToken 쿠키 설정
        ResponseCookie accessTokenCookie = createAccessTokenCookie(newAccessToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                .body(ApiResponse.success(null));
    }

    /**
     * JWT 토큰 검증 (WebSocket 인증용)
     *
     * @param request 토큰 검증 요청 DTO
     * @return 토큰 검증 결과
     */
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<UserValidationResponse>> validateToken(@Valid @RequestBody UserValidationRequest request) {
        UserValidationResponse response = authService.validateToken(request.getToken());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 쿠키 기반 JWT 토큰 검증 (WebSocket 인증용)
     *
     * @param accessToken HttpOnly 쿠키에서 추출한 AccessToken
     * @return 토큰 검증 결과
     */
    @PostMapping("/validate-cookie")
    public ResponseEntity<ApiResponse<UserValidationResponse>> validateTokenFromCookie(
            @CookieValue(name = "accessToken", required = false) String accessToken) {
        
        if (accessToken == null) {
            return ResponseEntity.ok(ApiResponse.success(new UserValidationResponse(false, null, null, null)));
        }
        
        UserValidationResponse response = authService.validateToken(accessToken);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * AccessToken HttpOnly 쿠키 생성 헬퍼 메서드
     *
     * @param accessToken JWT AccessToken 문자열
     * @return HttpOnly AccessToken 쿠키
     */
    private ResponseCookie createAccessTokenCookie(String accessToken) {
        return ResponseCookie.from("accessToken", accessToken)
                .httpOnly(true)                 // XSS 방지
                .secure(false)                  // 개발환경에서는 false, 운영환경에서는 true
                .sameSite("Lax")                // CSRF 방지
                .path("/")                      // 전체 경로에서 사용
                .maxAge(Duration.ofHours(1))    // 1시간 (향후 30분으로 변경 예정)
                .build();
    }

    private ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)                 // XSS 방지
                .secure(false)                  // 개발환경에서는 false, 운영환경에서는 true
                .sameSite("Lax")                // CSRF 방지
                .path("/")                      // 전체 경로에서 사용
                .maxAge(Duration.ofDays(7))     // 7일
                .build();
    }

    private ResponseCookie createExpiredCookie(String cookieName) {
        return ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)             // 즉시 만료
                .build();
    }
}
