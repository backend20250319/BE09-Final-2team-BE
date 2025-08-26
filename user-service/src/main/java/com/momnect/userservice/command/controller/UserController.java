package com.momnect.userservice.command.controller;

import com.momnect.userservice.command.dto.*;
import com.momnect.userservice.command.service.ChildService;
import com.momnect.userservice.command.service.UserService;
import com.momnect.userservice.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final ChildService childService; // ChildService를 주입받도록 수정

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
     * 마이페이지 메인 대시보드 정보(통합, 읽기 전용)
     */
    @Operation(summary = "마이페이지 대시보드 통합 정보", description = "프로필, 자녀정보, 거래 현황 등 대시보드에 필요한 모든 정보 조회")
    @GetMapping("/me/dashboard")
    public ResponseEntity<ApiResponse<MypageDTO>> getMypageDashboard(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);

        // 1. 프로필 정보 조회
        PublicUserDTO profileInfo = userService.getPublicUserProfile(userId);

        // 2. 자녀 정보 목록 조회
        List<ChildDTO> children = childService.getChildren(userId);

        // 3. 거래 현황 조회 (상품 서비스 및 리뷰 서비스 연동)
        // TODO: Feign Client를 통해 실제 거래 현황 데이터로 교체
        TransactionSummaryDTO transactionSummary = new TransactionSummaryDTO(); // 임시 DTO 생성
        transactionSummary.setTotalSalesCount(0);
        transactionSummary.setPurchaseCount(0);
        transactionSummary.setReviewCount(0);

        MypageDTO dashboardInfo = MypageDTO.builder()
                .profileInfo(profileInfo)
                .childList(children)
                .transactionSummary(transactionSummary)
                .build();

        return ResponseEntity.ok(ApiResponse.success(dashboardInfo));
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