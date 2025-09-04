package com.momnect.userservice.command.controller;

import com.momnect.userservice.command.dto.auth.ChangePasswordRequest;
import com.momnect.userservice.command.dto.child.ChildDTO;
import com.momnect.userservice.command.dto.common.*;
import com.momnect.userservice.command.dto.mypage.MypageDTO;
import com.momnect.userservice.command.dto.mypage.OtherUserProfileDTO;
import com.momnect.userservice.command.dto.user.*;
import com.momnect.userservice.command.service.ChildService;
import com.momnect.userservice.command.service.UserService;
import com.momnect.userservice.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
@Slf4j
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
        TransactionSummaryDTO transactionSummary = userService.getTransactionSummary(userId);

        // 리뷰 서비스가 아직 없으므로 임시로 0 설정
        transactionSummary.setReviewCount(0);

        MypageDTO dashboardInfo = userService.getMypageDashboard(userId);

        return ResponseEntity.ok(ApiResponse.success(dashboardInfo));
    }

    /**
     * 지역 검색 (emd 파라미터로)
     * URL: GET /users/search-areas?keyword=
     *
     * @param keyword 검색할 지역의 읍면동 이름
     * @return 검색된 지역 목록
     */
    @GetMapping("/search-areas")
    public ResponseEntity<ApiResponse<List<LocationSearchDTO>>> searchTradeLocations(@RequestParam("keyword") String keyword) {
        log.info("지역 검색 API 호출 - keyword: {}", keyword);
        // userService의 searchAreas 메서드 호출
        List<LocationSearchDTO> areas = userService.searchAreas(keyword);
        return ResponseEntity.ok(ApiResponse.success(areas));
    }

    /**
     * 내 거래지역 설정/수정
     */
    @PutMapping("/me/trade-locations")
    public ResponseEntity<ApiResponse<Void>> updateTradeLocations(
            @Valid @RequestBody UpdateTradeLocationsRequest request,
            HttpServletRequest httpRequest) {

        Long userId = getUserIdFromRequest(httpRequest);
        userService.updateTradeLocations(userId, request.getAreaIds());

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 내 거래지역 목록 조회
     * URL: GET /users/my-trade-locations
     *
     * @param userId 현재 로그인한 사용자 ID (실제로는 SecurityContext에서 가져옴)
     * @return 사용자가 설정한 거래지역 목록
     */
    @GetMapping("/{userId}/my-trade-locations")
    public ResponseEntity<ApiResponse<List<TradeLocationDTO>>> getMyTradeLocations(@PathVariable("userId") Long userId) {
        log.info("내 거래지역 목록 조회 API 호출 - userId: {}", userId);
        List<TradeLocationDTO> tradeLocations = userService.getMyTradeLocations(userId);
        return ResponseEntity.ok(ApiResponse.success(tradeLocations));
    }

    /**
     * 구매 상품 목록 조회 (상품 서비스 연동)
     */
    @Operation(summary = "구매 상품 목록 조회", description = "사용자가 구매한 상품 목록을 조회합니다.")
    @GetMapping("/me/products/purchased")
    public ResponseEntity<ApiResponse<List<ProductSummaryDTO>>> getPurchasedProducts(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        List<ProductSummaryDTO> products = userService.getPurchasedProducts(userId);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    /**
     * 판매 상품 목록 조회 (상품 서비스 연동)
     */
    @Operation(summary = "판매 상품 목록 조회", description = "사용자가 판매한 상품 목록을 조회합니다.")
    @GetMapping("/me/products/sold")
    public ResponseEntity<ApiResponse<List<ProductSummaryDTO>>> getSoldProducts(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        List<ProductSummaryDTO> products = userService.getSoldProducts(userId);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    /**
     * 타사용자 기본 정보 (닉네임, 이미지만)
     */
    @Operation(summary = "타사용자 기본 정보", description = "공개 프로필 정보를 조회합니다.")
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<PublicUserDTO>> getUserInfo(@PathVariable Long userId) {
        PublicUserDTO userInfo = userService.getPublicUserProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(userInfo));
    }

    /**
     * 타 사용자 프로필 페이지 정보 (통합, 읽기 전용)
     */
    @Operation(summary = "타 사용자 프로필 페이지 통합 정보", description = "프로필, 거래 현황, 판매 상품 목록 등 타 사용자 프로필에 필요한 모든 정보 조회")
    @GetMapping("/{userId}/profile-page")
    public ResponseEntity<ApiResponse<OtherUserProfileDTO>> getOtherUserProfile(@PathVariable Long userId) {
        OtherUserProfileDTO otherUserProfile = userService.getOtherUserProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(otherUserProfile));
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