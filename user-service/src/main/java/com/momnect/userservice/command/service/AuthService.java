package com.momnect.userservice.command.service;

import com.momnect.userservice.command.dto.auth.*;
import com.momnect.userservice.command.dto.child.ChildDTO;
import com.momnect.userservice.command.dto.common.TradeLocationDTO;
import com.momnect.userservice.command.dto.user.PublicUserDTO;
import com.momnect.userservice.command.entity.User;
import com.momnect.userservice.command.repository.UserRepository;
import com.momnect.userservice.exception.UserNotFoundException;
import com.momnect.userservice.jwt.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.momnect.userservice.command.mapper.UserMapper;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.List;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final ChildService childService;      // 자녀 정보 서비스
    private final UserService userService;       // 거래지역 조회용

    /**
     * 로그인: loginId + password 검증 후 AccessToken, RefreshToken 발급
     */
    public AuthResponseDTO login(LoginRequest request) {
        // DB에서 사용자 조회
        User user = userRepository.findByLoginId(request.getLoginId()) // DTO 사용
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 탈퇴한 사용자 확인
        if (user.getIsDeleted()) {
            throw new RuntimeException("탈퇴한 사용자입니다");
        }

        // 로그인 시 비밀번호 검증, 평문과 해시를 비교(복원X)
        // request.getPassword() - 사용자가 입력한 평문 비밀번호
        // user.getPassword() - DB에 저장된 해시된 비밀번호
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) { // DTO 사용
            throw new RuntimeException("Invalid password");
        }

        String accessToken = jwtTokenProvider.createAccessToken(user);
        String refreshToken = jwtTokenProvider.createRefreshToken(user);

        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        // 추가 정보 조회
        List<ChildDTO> childList = childService.getChildren(user.getId());
        List<TradeLocationDTO> tradeLocationList = userService.getMyTradeLocations(user.getId());

        PublicUserDTO publicUserDTO = userMapper.toPublicUserDTO(user,tradeLocationList.stream()
                .map(TradeLocationDTO::getEmd)
                .toList());

        return AuthResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(publicUserDTO)
                .childList(childList)                 // 실제 데이터 또는 빈 리스트
                .tradeLocationList(tradeLocationList) // 실제 데이터 또는 빈 리스트
                .build();
    }

    /**
     * 회원가입
     */
    public AuthResponseDTO signup(SignupRequest request) {
        // 중복 체크
        validateDuplicateUser(request);

        // 약관 동의 체크
        validateTermsAgreement(request);

        // User 엔티티 생성
        User user = createUserFromRequest(request);

        // 저장 후 생성자/수정자 ID 업데이트
        user = userRepository.save(user);
        user.setCreatedBy(user.getId());
        user.setUpdatedBy(user.getId());
        user = userRepository.save(user);

        // 토큰 발급
        String accessToken = jwtTokenProvider.createAccessToken(user);
        String refreshToken = jwtTokenProvider.createRefreshToken(user);

        // RefreshToken 저장
        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        PublicUserDTO publicUserDTO = userMapper.toPublicUserDTO(user,  Collections.emptyList());

        return AuthResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(publicUserDTO)
                .childList(Collections.emptyList())      // 빈 리스트
                .tradeLocationList(Collections.emptyList()) // 빈 리스트
                .build();
    }

    /**
     * 중복 사용자 검증
     */
    private void validateDuplicateUser(SignupRequest request) {
        if (userRepository.findByLoginId(request.getLoginId()).isPresent()) {
            throw new RuntimeException("이미 사용 중인 로그인 ID입니다");
        }

        // 이메일 중복 체크 추가
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("이미 사용 중인 이메일입니다");
        }
    }

    /**
     * 로그아웃 (RefreshToken 제거)
     */
    public void logout(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다"));

        user.setRefreshToken(null);
        user.setUpdatedBy(userId);
        userRepository.save(user);
    }

    /**
     * AccessToken 재발급
     */
    public String refreshAccessToken(String refreshToken) {
        // RefreshToken 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("유효하지 않은 Refresh token 입니다");
        }

        // DB에서 RefreshToken 확인
        User user = userRepository.findByRefreshToken(refreshToken).orElseThrow(()-> new RuntimeException("Refresh token을 찾을 수 없습니다"));

        if (user.getIsDeleted()) {
            throw new UserNotFoundException("탈퇴한 사용자입니다");
        }

        // 새로운 AccessToken 생성
        return jwtTokenProvider.createAccessToken(user);
    }

    /**
     * 약관 동의 검증
     */
    private void validateTermsAgreement(SignupRequest request) {
        if (!request.getIsTermsAgreed()) {
            throw new RuntimeException("이용약관 동의는 필수입니다");
        }
        if (!request.getIsPrivacyAgreed()) {
            throw new RuntimeException("개인정보처리방침 동의는 필수입니다");
        }
    }

    /**
     * SignupRequest에서 User 엔티티 생성
     */
    private User createUserFromRequest(SignupRequest request) {
        // 휴대폰번호 정제 (하이픈 제거)
        String cleanPhoneNumber = request.getPhoneNumber().replaceAll("[^0-9]", "");

        return User.builder()
                .loginId(request.getLoginId())
                .password(request.getPassword() != null ?
                        passwordEncoder.encode(request.getPassword()) : null)
                .name(request.getName())
                .email(request.getEmail())
                .phoneNumber(cleanPhoneNumber)
                .oauthProvider(request.getOauthProvider())
                .oauthId(request.getOauthId())
                .nickname(request.getNickname())
                .address(request.getAddress())
                .profileImageUrl(request.getProfileImageUrl())
                .role(request.getRole() != null ? request.getRole() : "USER")
                .isTermsAgreed(request.getIsTermsAgreed())
                .isPrivacyAgreed(request.getIsPrivacyAgreed())
                .isDeleted(false)
                .createdBy(0L) // 임시값, 저장 후 실제 ID로 업데이트
                .updatedBy(0L) // 임시값, 저장 후 실제 ID로 업데이트
                .build();
    }

    /**
     * 통합 계정 확인 (아이디 찾기 / 비밀번호 재설정)
     */
    public VerifyAccountResponse verifyAccount(VerifyAccountRequest request) {
        log.info("계정 확인 요청: type={}, email={}", request.getType(), request.getEmail());

        // 입력값 검증
        validateVerifyRequest(request);

        if ("FIND_ID".equals(request.getType())) {
            return handleFindId(request);
        } else if ("RESET_PASSWORD".equals(request.getType())) {
            return handleResetPassword(request);
        } else {
            throw new IllegalArgumentException("지원하지 않는 요청 타입입니다: " + request.getType());
        }
    }

    /**
     * 비밀번호 재설정
     */
    public void resetPassword(ResetPasswordRequest request) {
        log.info("비밀번호 재설정 요청");

        // 비밀번호 일치 확인
        if (!request.isPasswordMatched()) {
            throw new IllegalArgumentException("새 비밀번호가 일치하지 않습니다");
        }

        // 토큰 검증 및 사용자 ID 추출
        Long userId = jwtTokenProvider.getUserIdFromPasswordResetToken(request.getResetToken());

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        if (user.getIsDeleted()) {
            throw new RuntimeException("탈퇴한 사용자입니다");
        }

        // 비밀번호 변경
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedBy(userId);
        userRepository.save(user);

        log.info("비밀번호 재설정 완료: userId={}", userId);
    }

    /**
     * 아이디 찾기 처리
     */
    private VerifyAccountResponse handleFindId(VerifyAccountRequest request) {
        // 이름과 이메일로 사용자 찾기
        User user = userRepository.findByEmail(request.getEmail())
                .filter(u -> !u.getIsDeleted())
                .filter(u -> request.getName().equals(u.getName()))
                .orElseThrow(() -> new RuntimeException("일치하는 사용자 정보를 찾을 수 없습니다"));

        log.info("아이디 찾기 완료: userId={}", user.getId());
        return VerifyAccountResponse.findIdSuccess(user.getLoginId());
    }

    /**
     * 비밀번호 재설정 처리
     */
    private VerifyAccountResponse handleResetPassword(VerifyAccountRequest request) {
        // 아이디와 이메일로 사용자 찾기
        User user = userRepository.findByLoginId(request.getLoginId())
                .filter(u -> !u.getIsDeleted())
                .filter(u -> request.getEmail().equals(u.getEmail()))
                .orElseThrow(() -> new RuntimeException("일치하는 사용자 정보를 찾을 수 없습니다"));

        // 소셜 로그인 사용자는 비밀번호 재설정 불가
        if (!"LOCAL".equals(user.getOauthProvider())) {
            throw new RuntimeException("소셜 로그인 사용자는 비밀번호를 재설정할 수 없습니다");
        }

        // 재설정 토큰 생성
        String resetToken = jwtTokenProvider.createPasswordResetToken(user.getId());

        log.info("비밀번호 재설정 토큰 생성 완료: userId={}", user.getId());
        return VerifyAccountResponse.resetPasswordSuccess(resetToken);
    }

    /**
     * 계정 확인 요청 검증
     */
    private void validateVerifyRequest(VerifyAccountRequest request) {
        if ("FIND_ID".equals(request.getType())) {
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("아이디 찾기 시 이름은 필수입니다");
            }
        } else if ("RESET_PASSWORD".equals(request.getType())) {
            if (request.getLoginId() == null || request.getLoginId().trim().isEmpty()) {
                throw new IllegalArgumentException("비밀번호 재설정 시 아이디는 필수입니다");
            }
        }
    }

    /**
     * JWT 토큰 검증 (WebSocket 인증용)
     */
    public UserValidationResponse validateToken(String token) {
        try {
            // JWT 토큰 검증
            if (!jwtTokenProvider.validateToken(token)) {
                return new UserValidationResponse(false, null, null, null);
            }

            // 토큰에서 사용자 ID 추출
            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            
            // 사용자 조회
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

            if (user.getIsDeleted()) {
                return new UserValidationResponse(false, null, null, null);
            }

            // 사용자 역할 정보 (기본값: USER)
            List<String> roles = List.of("USER");

            return new UserValidationResponse(
                true, 
                userId.toString(), 
                user.getName(), 
                roles
            );

        } catch (Exception e) {
            log.error("Token validation failed", e);
            return new UserValidationResponse(false, null, null, null);
        }
    }
}