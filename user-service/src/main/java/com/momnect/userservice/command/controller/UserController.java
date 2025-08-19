package com.momnect.userservice.command.controller;

import com.momnect.userservice.command.dto.*;
import com.momnect.userservice.command.service.UserService;
import com.momnect.userservice.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    /**
     * 헤더용 기본 정보 (닉네임, 이미지만)
     */
    @GetMapping("/{userId}/basic")
    public ResponseEntity<ApiResponse<PublicUserDTO>> getBasicInfo(@PathVariable Long userId) {
        PublicUserDTO userInfo = userService.getPublicUserProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(userInfo));
    }

    /**
     * 사용자 존재 여부 확인
     */
    @GetMapping("/{userId}/exists")
    public ResponseEntity<ApiResponse<Boolean>> checkUserExists(@PathVariable Long userId) {
        boolean exists = userService.checkUserExists(userId);
        return ResponseEntity.ok(ApiResponse.success(exists));
    }

    /**
     * 마이페이지 기본 정보 (닉네임, 이미지만) - 프론트엔드용
     * 자녀정보는 자녀관리 API 완성 후 제거 예정
     */
    @Operation(summary = "마이페이지 기본 정보", description = "프로필 정보를 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PublicUserDTO>> getMyInfo(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        PublicUserDTO userInfo = userService.getPublicUserProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(userInfo));

        // TODO: 자녀관리 API 완성 후 자녀 정보 추가
        // TODO: 프론트엔드에서 자녀관리 API 별도 호출로 변경
    }

    /**
     * 타사용자 기본 정보 (닉네임, 이미지만) - 프론트엔드용
     */
    @Operation(summary = "타사용자 기본 정보", description = "공개 프로필 정보를 조회합니다.")
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<PublicUserDTO>> getUserInfo(@PathVariable Long userId) {
        PublicUserDTO userInfo = userService.getPublicUserProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(userInfo));
    }

    /**
     * 프로필 수정용 정보 조회
     */
    @GetMapping("/me/profile")
    public ResponseEntity<ApiResponse<UserDTO>> getProfileForEdit(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        UserDTO profileInfo = userService.getProfileForEdit(userId);
        return ResponseEntity.ok(ApiResponse.success(profileInfo));
    }

    /**
     * 프로필 수정
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserDTO>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        UserDTO updatedUser = userService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success(updatedUser));
    }

    /**
     * 비밀번호 변경
     */
    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest) {
        if (!request.isPasswordMatched()) {
            throw new RuntimeException("새 비밀번호가 일치하지 않습니다.");
        }
        Long userId = getUserIdFromRequest(httpRequest);
        userService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 회원탈퇴
     */
    @DeleteMapping("/account")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @Valid @RequestBody DeleteAccountRequest request,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        userService.deleteAccount(userId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 중복 확인
     */
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<CheckDuplicateResponse>> checkDuplicate(
            @RequestParam String type,
            @RequestParam String value) {
        boolean isDuplicate = userService.checkDuplicate(type, value);
        CheckDuplicateResponse response = new CheckDuplicateResponse(type, value, isDuplicate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader != null) {
            return Long.parseLong(userIdHeader);
        }

        Object userIdAttr = request.getAttribute("X-User-Id");
        if (userIdAttr != null) {
            return Long.parseLong(userIdAttr.toString());
        }

        throw new RuntimeException("사용자 인증 정보를 찾을 수 없습니다.");
    }
}