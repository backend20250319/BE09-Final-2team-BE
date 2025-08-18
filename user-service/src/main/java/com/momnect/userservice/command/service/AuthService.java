package com.momnect.userservice.command.service;

import com.momnect.userservice.command.dto.LoginResponse;
import com.momnect.userservice.command.dto.SignupRequest;
import com.momnect.userservice.command.dto.UserDTO;
import com.momnect.userservice.command.entity.User;
import com.momnect.userservice.command.repository.UserRepository;
import com.momnect.userservice.exception.UserNotFoundException;
import com.momnect.userservice.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    /**
     * 로그인: loginId + password 검증 후 AccessToken, RefreshToken 발급
     */
    public LoginResponse login(String loginId, String password) {
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        String accessToken = jwtTokenProvider.createAccessToken(user);
        String refreshToken = jwtTokenProvider.createRefreshToken(user);

        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        UserDTO userDTO = convertToUserDTO(user);
        return new LoginResponse(accessToken, refreshToken, userDTO);
    }

    /**
     * 회원가입
     */
    public LoginResponse signup(SignupRequest request) {
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

        UserDTO userDTO = convertToUserDTO(user);
        return new LoginResponse(accessToken, refreshToken, userDTO);
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
    public String refreshToken(String refreshToken) {
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
     * User 엔티티를 UserDTO로 변환
     */
    private UserDTO convertToUserDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .loginId(user.getLoginId())
                .name(user.getName())
                .role(user.getRole())
                .oauthProvider(user.getOauthProvider())
                .oauthId(user.getOauthId())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .address(user.getAddress())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .deletedAt(user.getDeletedAt())
                .isDeleted(user.getIsDeleted())
                .isTermsAgreed(user.getIsTermsAgreed())
                .isPrivacyAgreed(user.getIsPrivacyAgreed())
                .isWithdrawalAgreed(user.getIsWithdrawalAgreed())
                .createdBy(user.getCreatedBy())
                .updatedBy(user.getUpdatedBy())
                .build();
    }
}